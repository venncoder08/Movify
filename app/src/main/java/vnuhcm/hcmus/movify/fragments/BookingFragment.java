package vnuhcm.hcmus.movify.fragments;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.Manifest;
import android.view.Gravity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vnuhcm.hcmus.movify.R;
import vnuhcm.hcmus.movify.adapters.PromotionAdapter;
import vnuhcm.hcmus.movify.models.AccountProfile;
import vnuhcm.hcmus.movify.models.Promotion;
import vnuhcm.hcmus.movify.models.RoutePreview;
import vnuhcm.hcmus.movify.services.AuthApiService;
import vnuhcm.hcmus.movify.services.PromotionService;
import vnuhcm.hcmus.movify.services.RouteService;
import vnuhcm.hcmus.movify.utils.CurrentLocationResolver;
import vnuhcm.hcmus.movify.utils.DriverSearchNotificationHelper;
import vnuhcm.hcmus.movify.utils.LocationData;
import vnuhcm.hcmus.movify.utils.PlacesAutocompleteHelper;
import vnuhcm.hcmus.movify.services.TripService;

public class BookingFragment extends Fragment {

    private static final String ARG_IS_DRIVER_MODE = "is_driver_mode";
    private static final String TAG = "BookingFragment";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 201;
    private static final String VEHICLE_MOTORBIKE = "motorbike";
    private static final String VEHICLE_CAR = "car";
    private static final String PAYMENT_CASH = "cash";
    private static final String PAYMENT_BANK_TRANSFER = "bank_transfer";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<Promotion> promotionList = new ArrayList<>();

    private AutoCompleteTextView edtPickup;
    private AutoCompleteTextView edtDestination;
    private View headerLayout;
    private LinearLayout bookingContentRoot;
    private LinearLayout locationContainer;
    private LinearLayout pickupContainer;
    private LinearLayout destinationContainer;

    private LinearLayout vehicleContainer;
    private LinearLayout btnMotorbike;
    private LinearLayout btnCar;
    private LinearLayout btnCash;
    private LinearLayout btnBankTransfer;
    private LinearLayout btnSearchDriver;
    private LinearLayout promoContainer;
    private LinearLayout driverModeContainer;
    private FrameLayout iconMotorbike;
    private FrameLayout iconCar;
    private FrameLayout iconCash;
    private FrameLayout iconBank;
    private ImageView ivMotorbike;
    private ImageView ivCar;
    private ImageView ivCash;
    private ImageView ivBankTransfer;
    private ImageView ivQr;
    private TextView tvBikeTitle;
    private TextView tvBikeDesc;
    private TextView tvCarTitle;
    private TextView tvCarDesc;
    private TextView tvCash;
    private TextView tvBankTransfer;
    private TextView tvWelcome;
    private TextView tvHeaderUserName;
    private LinearLayout routeSummaryContainer;
    private TextView tvRouteDistance;
    private TextView tvRouteDuration;
    private TextView tvRouteAmount;
    private TextView tvChooseVehicle;
    private TextView tvPaymentTitle;
    private TextView tvPromoTitle;
    private RecyclerView rvPromotions;

    private PromotionAdapter promotionAdapter;
    private ExecutorService promotionExecutor;
    private ExecutorService routeExecutor;
    private AuthApiService authApiService;

    private boolean isDriverMode;
    private RoutePreview currentRoutePreview;
    private Runnable pendingRouteUpdate;
    private AutoCompleteTextView pendingCurrentLocationField;
    private vnuhcm.hcmus.movify.models.Promotion selectedPromotion = null;
    private String selectedVehicle = VEHICLE_MOTORBIKE;
    private String selectedPayment = PAYMENT_CASH;

    // Driver-search countdown
    private CountDownTimer searchCountdownTimer;
    private boolean isSearchingDriver = false;
    private String currentTripId = null;   // backend-assigned id after createTrip succeeds
    private static final long SEARCH_TIMEOUT_MS = 10L * 60L * 1000L; // 10 minutes max

    public BookingFragment() {
    }

    public static BookingFragment newInstance(boolean isDriverMode) {
        BookingFragment fragment = new BookingFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_DRIVER_MODE, isDriverMode);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_booking, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            isDriverMode = getArguments().getBoolean(ARG_IS_DRIVER_MODE, false);
        }

        promotionExecutor = Executors.newSingleThreadExecutor();
        routeExecutor = Executors.newSingleThreadExecutor();
        authApiService = new AuthApiService(requireContext());
        initViews(view);
        renderHeaderLoading();

        String pickupLocation = LocationData.getPickupLocation();
        String destinationLocation = LocationData.getDestinationLocation();

        loadHeaderProfile();

        if (isDriverMode) {
            applyDriverMode();
            mainHandler.post(this::openMapBooking);
        } else {
            setupVehicleButtons();
            setupPaymentButtons();
            setupPromotions();
            setupSearchDriverButton();
            setupPlacesAutocomplete();
            attachRoutePreviewWatchers();
        }
            if (!isDriverMode && pickupLocation != null && !pickupLocation.isEmpty()) {
                edtPickup.setText(pickupLocation);
            }
            if (!isDriverMode && destinationLocation != null && !destinationLocation.isEmpty()) {
                edtDestination.setText(destinationLocation);
            }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        stopSearchCountdown(false);
        mainHandler.removeCallbacksAndMessages(null);
        if (promotionExecutor != null) {
            promotionExecutor.shutdownNow();
            promotionExecutor = null;
        }
        if (routeExecutor != null) {
            routeExecutor.shutdownNow();
            routeExecutor = null;
        }
        pendingCurrentLocationField = null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (pendingCurrentLocationField != null) {
                resolveAndFillCurrentLocation(pendingCurrentLocationField);
            }
        } else {
            Toast.makeText(requireContext(), "Cần quyền vị trí để dùng option này", Toast.LENGTH_SHORT).show();
        }

        pendingCurrentLocationField = null;
    }

    private void initViews(View view) {
        edtPickup = view.findViewById(R.id.edtPickup);
        edtDestination = view.findViewById(R.id.edtDestination);
        headerLayout = view.findViewById(R.id.headerLayout);
        bookingContentRoot = view.findViewById(R.id.bookingContentRoot);
        locationContainer = view.findViewById(R.id.locationContainer);
        pickupContainer = view.findViewById(R.id.pickupContainer);
        destinationContainer = view.findViewById(R.id.destinationContainer);

        vehicleContainer = view.findViewById(R.id.vehicleContainer);
        btnMotorbike = view.findViewById(R.id.btnMotorbike);
        btnCar = view.findViewById(R.id.btnCar);
        btnCash = view.findViewById(R.id.btnCash);
        btnBankTransfer = view.findViewById(R.id.btnBankTransfer);
        btnSearchDriver = view.findViewById(R.id.btnSearchDriver);
        promoContainer = view.findViewById(R.id.promoContainer);
        driverModeContainer = view.findViewById(R.id.driverModeContainer);

        iconMotorbike = view.findViewById(R.id.iconMotorbike);
        iconCar = view.findViewById(R.id.iconCar);
        iconCash = view.findViewById(R.id.iconCash);
        iconBank = view.findViewById(R.id.iconBank);

        ivMotorbike = view.findViewById(R.id.ivMotorbike);
        ivCar = view.findViewById(R.id.ivCar);
        ivCash = view.findViewById(R.id.ivCash);
        ivBankTransfer = view.findViewById(R.id.ivBankTransfer);
        ivQr = view.findViewById(R.id.ivQr);

        tvBikeTitle = view.findViewById(R.id.tvBikeTitle);
        tvBikeDesc = view.findViewById(R.id.tvBikeDesc);
        tvCarTitle = view.findViewById(R.id.tvCarTitle);
        tvCarDesc = view.findViewById(R.id.tvCarDesc);
        tvCash = view.findViewById(R.id.tvCash);
        tvBankTransfer = view.findViewById(R.id.tvBankTransfer);
        tvWelcome = view.findViewById(R.id.tvWelcome);
        tvHeaderUserName = view.findViewById(R.id.tvUserName);
        routeSummaryContainer = view.findViewById(R.id.routeSummaryContainer);
        tvRouteDistance = view.findViewById(R.id.tvRouteDistance);
        tvRouteDuration = view.findViewById(R.id.tvRouteDuration);
        tvRouteAmount = view.findViewById(R.id.tvRouteAmount);
        tvChooseVehicle = view.findViewById(R.id.tvChooseVehicle);
        tvPaymentTitle = view.findViewById(R.id.tvPaymentTitle);
        tvPromoTitle = view.findViewById(R.id.tvPromoTitle);

        rvPromotions = view.findViewById(R.id.rvPromotions);
    }

    private void loadHeaderProfile() {
        if (promotionExecutor == null || promotionExecutor.isShutdown()) {
            return;
        }

        promotionExecutor.execute(() -> {
            try {
                AccountProfile profile = authApiService.fetchCurrentUserProfile();
                mainHandler.post(() -> renderHeaderProfile(profile));
            } catch (Exception exception) {
                Log.w(TAG, "Unable to load booking header profile", exception);
                mainHandler.post(this::renderHeaderFallback);
            }
        });
    }

    private void renderHeaderLoading() {
        tvHeaderUserName.setText(R.string.loading_dots);
    }

    private void renderHeaderProfile(@NonNull AccountProfile profile) {
        if (!isAdded()) {
            return;
        }

        String displayName = profile.getFullName();
        if (TextUtils.isEmpty(displayName)) {
            displayName = buildDisplayName(profile.getEmail());
        }

        tvHeaderUserName.setText(displayName);
    }

    private void renderHeaderFallback() {
        if (!isAdded()) {
            return;
        }

        tvHeaderUserName.setText(buildDisplayName(authApiService.getUserEmail()));
    }

    private void applyDriverMode() {
        tvWelcome.setText(R.string.driver_welcome);
        ivQr.setVisibility(View.GONE);
        setVisible(headerLayout, false);
        bookingContentRoot.setGravity(Gravity.CENTER);

        setVisible(driverModeContainer, true);
        setVisible(locationContainer, false);
        setVisible(tvChooseVehicle, false);
        setVisible(vehicleContainer, false);
        setVisible(tvPaymentTitle, false);
        setVisible(btnCash, false);
        setVisible(btnBankTransfer, false);
        setVisible(tvPromoTitle, false);
        setVisible(promoContainer, false);
        setVisible(btnSearchDriver, false);
        setVisible(routeSummaryContainer, false);
    }

    private void setVisible(View view, boolean visible) {
        if (view != null) {
            view.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private String buildDisplayName(String email) {
        if (TextUtils.isEmpty(email)) {
            return getString(R.string.movify_account);
        }

        int separatorIndex = email.indexOf('@');
        if (separatorIndex > 0) {
            return email.substring(0, separatorIndex);
        }

        return email;
    }

    private void setupVehicleButtons() {
        updateVehicleUI();

        btnMotorbike.setOnClickListener(v -> {
            selectedVehicle = VEHICLE_MOTORBIKE;
            updateVehicleUI();
            renderRouteSummary(currentRoutePreview);
        });

        btnCar.setOnClickListener(v -> {
            selectedVehicle = VEHICLE_CAR;
            updateVehicleUI();
            renderRouteSummary(currentRoutePreview);
        });
    }

    private void updateVehicleUI() {
        if (VEHICLE_MOTORBIKE.equals(selectedVehicle)) {
            btnMotorbike.setBackgroundResource(R.drawable.bg_selected);
            btnCar.setBackgroundResource(R.drawable.bg_unselected);

            iconMotorbike.setBackgroundResource(R.drawable.bg_icon_blue_soft);
            iconCar.setBackgroundResource(R.drawable.bg_input);

            ivMotorbike.setColorFilter(ContextCompat.getColor(requireContext(), android.R.color.white));
            ivCar.setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary));

            tvBikeTitle.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
            tvBikeDesc.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
            tvCarTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            tvCarDesc.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        } else {
            btnMotorbike.setBackgroundResource(R.drawable.bg_unselected);
            btnCar.setBackgroundResource(R.drawable.bg_selected);

            iconMotorbike.setBackgroundResource(R.drawable.bg_input);
            iconCar.setBackgroundResource(R.drawable.bg_icon_blue_soft);

            ivMotorbike.setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary));
            ivCar.setColorFilter(ContextCompat.getColor(requireContext(), android.R.color.white));

            tvBikeTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            tvBikeDesc.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            tvCarTitle.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
            tvCarDesc.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
        }
    }

    private void setupPaymentButtons() {
        updatePaymentUI();

        btnCash.setOnClickListener(v -> {
            selectedPayment = PAYMENT_CASH;
            updatePaymentUI();
        });

        btnBankTransfer.setOnClickListener(v -> {
            selectedPayment = PAYMENT_BANK_TRANSFER;
            updatePaymentUI();
        });
    }

    private void updatePaymentUI() {
        if (PAYMENT_CASH.equals(selectedPayment)) {
            btnCash.setBackgroundResource(R.drawable.bg_selected);
            btnBankTransfer.setBackgroundResource(R.drawable.bg_unselected);

            iconCash.setBackgroundResource(R.drawable.bg_icon_blue_soft);
            iconBank.setBackgroundResource(R.drawable.bg_input);

            ivCash.setColorFilter(ContextCompat.getColor(requireContext(), android.R.color.white));
            ivBankTransfer.setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary));

            tvCash.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
            tvBankTransfer.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        } else {
            btnCash.setBackgroundResource(R.drawable.bg_unselected);
            btnBankTransfer.setBackgroundResource(R.drawable.bg_selected);

            iconCash.setBackgroundResource(R.drawable.bg_input);
            iconBank.setBackgroundResource(R.drawable.bg_icon_blue_soft);

            ivCash.setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary));
            ivBankTransfer.setColorFilter(ContextCompat.getColor(requireContext(), android.R.color.white));

            tvCash.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            tvBankTransfer.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
        }
    }

    private void setupPromotions() {
        rvPromotions.setLayoutManager(new LinearLayoutManager(requireContext()));
        promotionAdapter = new PromotionAdapter(promotionList, promotion -> {
            selectedPromotion = promotion; // may be null when deselected
            if (promotion != null) {
                Toast.makeText(requireContext(), getString(R.string.selected_promo) + promotion.getTitle(), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), getString(R.string.promo_deselected), Toast.LENGTH_SHORT).show();
            }
            renderRouteSummary(currentRoutePreview);
        });
        rvPromotions.setAdapter(promotionAdapter);
        loadPromotions();
    }

    private void loadPromotions() {
        if (promotionExecutor == null || promotionExecutor.isShutdown()) {
            return;
        }

        Context appContext = requireContext().getApplicationContext();
        promotionExecutor.execute(() -> {
            try {
                List<Promotion> promotions = PromotionService.fetchPromotions(appContext);
                mainHandler.post(() -> renderPromotions(promotions));
            } catch (Exception exception) {
                Log.e(TAG, "Unable to load promotions from backend", exception);
                mainHandler.post(() -> {
                    if (!isAdded()) {
                        return;
                    }
                    Toast.makeText(requireContext(), R.string.load_promo_failed, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void renderPromotions(@NonNull List<Promotion> promotions) {
        if (!isAdded()) {
            return;
        }

        promotionList.clear();
        promotionList.addAll(promotions);
        promotionAdapter.notifyDataSetChanged();
    }

    private void attachPlacesAutocomplete() {
        new PlacesAutocompleteHelper(requireContext(), edtPickup, this::handleCurrentLocationSelection);
        new PlacesAutocompleteHelper(requireContext(), edtDestination, this::handleCurrentLocationSelection);

        edtPickup.setOnClickListener(v -> openMapBooking());
        edtDestination.setOnClickListener(v -> openMapBooking());
        pickupContainer.setOnClickListener(v -> openMapBooking());
        destinationContainer.setOnClickListener(v -> openMapBooking());

        edtPickup.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                openMapBooking();
            }
        });

        edtDestination.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                openMapBooking();
            }
        });
    }

    private void handleCurrentLocationSelection(@NonNull AutoCompleteTextView targetField) {
        pendingCurrentLocationField = targetField;

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        resolveAndFillCurrentLocation(targetField);
    }

    private void resolveAndFillCurrentLocation(@NonNull AutoCompleteTextView targetField) {
        if (routeExecutor == null || routeExecutor.isShutdown()) {
            return;
        }

        Context appContext = requireContext().getApplicationContext();
        routeExecutor.execute(() -> {
            try {
                String currentLocationLabel = CurrentLocationResolver.resolveCurrentLocationLabel(appContext);
                mainHandler.post(() -> {
                    if (!isAdded()) {
                        return;
                    }

                    targetField.setText(currentLocationLabel);
                });
            } catch (Exception exception) {
                Log.w(TAG, "Unable to resolve current location", exception);
                mainHandler.post(() -> {
                    if (!isAdded()) {
                        return;
                    }

                    Toast.makeText(requireContext(), "Không thể lấy vị trí hiện tại", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void attachRoutePreviewWatchers() {
        android.text.TextWatcher routeWatcher = new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                scheduleRoutePreviewUpdate();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };

        edtPickup.addTextChangedListener(routeWatcher);
        edtDestination.addTextChangedListener(routeWatcher);

        scheduleRoutePreviewUpdate();
    }

    private void scheduleRoutePreviewUpdate() {
        if (routeExecutor == null || routeExecutor.isShutdown()) {
            return;
        }

        if (pendingRouteUpdate != null) {
            mainHandler.removeCallbacks(pendingRouteUpdate);
        }

        pendingRouteUpdate = () -> updateRoutePreview();
        mainHandler.postDelayed(pendingRouteUpdate, 450L);
    }

    private void updateRoutePreview() {
        if (routeExecutor == null || routeExecutor.isShutdown() || !isAdded()) {
            return;
        }

        final String pickup = edtPickup.getText().toString().trim();
        final String destination = edtDestination.getText().toString().trim();

        if (TextUtils.isEmpty(pickup) || TextUtils.isEmpty(destination)) {
            currentRoutePreview = null;
            renderRouteSummary(null);
            return;
        }

        Context appContext = requireContext().getApplicationContext();
        routeExecutor.execute(() -> {
            try {
                RoutePreview routePreview = RouteService.fetchRoutePreview(appContext, pickup, destination);
                mainHandler.post(() -> {
                    if (isAdded()) {
                        currentRoutePreview = routePreview;
                        renderRouteSummary(routePreview);
                    }
                });
            } catch (Exception exception) {
                Log.w(TAG, "Unable to load route preview", exception);
                mainHandler.post(() -> {
                    if (isAdded()) {
                        currentRoutePreview = null;
                        renderRouteSummary(null);
                    }
                });
            }
        });
    }

    private void renderRouteSummary(@Nullable RoutePreview routePreview) {
        if (!isAdded()) {
            return;
        }

        if (routePreview == null) {
            setVisible(routeSummaryContainer, false);
            return;
        }

        double distanceKm = routePreview.distanceMeters / 1000d;
        double durationMinutes = routePreview.durationSeconds / 60d;
        double baseFare = estimateFare(distanceKm, selectedVehicle);
        double discount = 0d;
        if (selectedPromotion != null) {
            if ("percent".equalsIgnoreCase(selectedPromotion.getType())) {
                discount = baseFare * (selectedPromotion.getDiscountValue() / 100d);
            } else {
                discount = selectedPromotion.getDiscountValue();
            }
        }
        double finalAmountVal = Math.max(0d, baseFare - discount);
        String totalAmount = formatCurrency(finalAmountVal);

        tvRouteDistance.setText(getString(R.string.route_distance_label) + ": " + formatDistance(distanceKm));
        tvRouteDuration.setText(getString(R.string.route_duration_label) + ": " + formatDuration(durationMinutes));
        tvRouteAmount.setText(getString(R.string.route_amount_label) + ": " + totalAmount);
            // note view removed; nothing to do here
        setVisible(routeSummaryContainer, true);
    }

    private static double estimateFare(double distanceKm, String vehicleType) {
        if (VEHICLE_CAR.equals(vehicleType)) {
            return 12000d + (distanceKm * 6500d);
        }

        return 8000d + (distanceKm * 4500d);
    }

    @NonNull
    private static String formatDistance(double distanceKm) {
        return String.format(Locale.US, "%.1f km", distanceKm);
    }

    @NonNull
    private static String formatDuration(double durationMinutes) {
        long roundedMinutes = Math.max(1L, Math.round(durationMinutes));
        return roundedMinutes + " phút";
    }

    @NonNull
    private static String formatCurrency(double amount) {
        NumberFormat formatter = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        formatter.setMaximumFractionDigits(0);
        formatter.setMinimumFractionDigits(0);
        return formatter.format(amount) + "đ";
    }

    private void setupSearchDriverButton() {
        btnSearchDriver.setOnClickListener(v -> {
            if (isSearchingDriver) {
                stopSearchCountdown(true);
                return;
            }

            String pickup = edtPickup.getText().toString().trim();
            String destination = edtDestination.getText().toString().trim();

            if (TextUtils.isEmpty(pickup)) {
                edtPickup.setError(getString(R.string.pickup_location_required));
                return;
            }

            if (TextUtils.isEmpty(destination)) {
                edtDestination.setError(getString(R.string.destination_required));
                return;
            }

            createTripThenSearch();
        });
    }

    /**
     * Step 1: create the trip on the backend, then start the countdown on success.
     */
    private void createTripThenSearch() {
        if (!isAdded()) return;

        // Show a temporary "creating…" state on the button
        setSearchButtonSearching(getString(R.string.trip_creating));
        btnSearchDriver.setEnabled(false);

        final String pickup      = edtPickup.getText().toString().trim();
        final String destination = edtDestination.getText().toString().trim();

        // Grab coordinates from the cached RoutePreview if available
        double pickupLat  = 0, pickupLng  = 0;
        double destLat    = 0, destLng    = 0;
        int    distMeters = 0;
        double estFare    = 0;
        if (currentRoutePreview != null) {
            if (currentRoutePreview.pickup != null && currentRoutePreview.pickup.latLng != null) {
                pickupLat = currentRoutePreview.pickup.latLng.latitude;
                pickupLng = currentRoutePreview.pickup.latLng.longitude;
            }
            if (currentRoutePreview.destination != null && currentRoutePreview.destination.latLng != null) {
                destLat = currentRoutePreview.destination.latLng.latitude;
                destLng = currentRoutePreview.destination.latLng.longitude;
            }
            distMeters = currentRoutePreview.distanceMeters;
            estFare    = estimateFare(distMeters / 1000d, selectedVehicle);
            if (selectedPromotion != null) {
                double discount = "percent".equalsIgnoreCase(selectedPromotion.getType())
                        ? estFare * (selectedPromotion.getDiscountValue() / 100d)
                        : selectedPromotion.getDiscountValue();
                estFare = Math.max(0d, estFare - discount);
            }
        }

        final int    promoId    = selectedPromotion != null ? selectedPromotion.getId() : -1;
        final double finalFare = estFare;
        final int    finalDist = distMeters;
        final double fPickupLat = pickupLat, fPickupLng = pickupLng;
        final double fDestLat   = destLat,   fDestLng   = destLng;

        if (routeExecutor == null || routeExecutor.isShutdown()) return;

        routeExecutor.execute(() -> {
            try {
                vnuhcm.hcmus.movify.models.Trip trip = TripService.createTrip(
                        requireContext().getApplicationContext(),
                        pickup, fPickupLat, fPickupLng,
                        destination, fDestLat, fDestLng,
                        selectedVehicle, selectedPayment,
                        promoId, finalFare, finalDist
                );
                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    currentTripId = trip.getId();
                    btnSearchDriver.setEnabled(true);
                    startSearchCountdown();
                });
            } catch (Exception e) {
                Log.e(TAG, "createTrip failed", e);
                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    btnSearchDriver.setEnabled(true);
                    // Reset button label
                    setSearchButtonLabel(R.string.find_driver);
                    Toast.makeText(requireContext(),
                            getString(R.string.trip_create_failed) + ": " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void startSearchCountdown() {
        if (!isAdded()) return;

        isSearchingDriver = true;
        DriverSearchNotificationHelper.start(requireContext().getApplicationContext());

        searchCountdownTimer = new CountDownTimer(SEARCH_TIMEOUT_MS, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (!isAdded()) return;
                long elapsedMs = DriverSearchNotificationHelper.getElapsedMillis();
                long elapsedSec = elapsedMs / 1000L;
                long minutes = elapsedSec / 60L;
                long seconds = elapsedSec % 60L;
                String label = getString(R.string.search_driver_in_progress,
                        String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds));
                setSearchButtonSearching(label);
                DriverSearchNotificationHelper.update(requireContext().getApplicationContext());
            }

            @Override
            public void onFinish() {
                if (!isAdded()) return;
                stopSearchCountdown(false);
                Toast.makeText(requireContext(), getString(R.string.search_driver_timeout), Toast.LENGTH_LONG).show();
            }
        };
        searchCountdownTimer.start();

        // Show initial label immediately
        setSearchButtonSearching(getString(R.string.search_driver_in_progress, "00:00"));
    }

    private void stopSearchCountdown(boolean userCancelled) {
        isSearchingDriver = false;
        if (searchCountdownTimer != null) {
            searchCountdownTimer.cancel();
            searchCountdownTimer = null;
        }
        if (isAdded()) {
            setSearchButtonLabel(R.string.find_driver);
            if (userCancelled) {
                Toast.makeText(requireContext(), getString(R.string.search_driver_cancelled), Toast.LENGTH_SHORT).show();
            }
        }
        // Cancel trip on backend if we have an id
        final String tripId = currentTripId;
        currentTripId = null;
        if (tripId != null && getContext() != null) {
            final Context appCtx = requireContext().getApplicationContext();
            if (routeExecutor != null && !routeExecutor.isShutdown()) {
                routeExecutor.execute(() -> {
                    try {
                        TripService.cancelTrip(appCtx, tripId);
                    } catch (Exception ignored) {
                        // Best-effort cancel; don't crash the UI
                    }
                });
            }
        }
        if (getContext() != null) {
            DriverSearchNotificationHelper.stop(requireContext().getApplicationContext());
        }
    }

    private void setSearchButtonLabel(int stringResId) {
        String text = isAdded() ? getString(stringResId) : null;
        if (text != null) setSearchButtonSearching(text);
    }

    private void setSearchButtonSearching(@NonNull String label) {
        // Try tag first, then fallback to first TextView child
        TextView tvBtn = btnSearchDriver.findViewWithTag("search_label");
        if (tvBtn != null) {
            tvBtn.setText(label);
            return;
        }
        for (int i = 0; i < btnSearchDriver.getChildCount(); i++) {
            View child = btnSearchDriver.getChildAt(i);
            if (child instanceof TextView) {
                ((TextView) child).setText(label);
                return;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (isDriverMode) {
            return;
        }

        String pickupLocation = LocationData.getPickupLocation();
        String destinationLocation = LocationData.getDestinationLocation();
        boolean updated = false;

        if (pickupLocation != null && !pickupLocation.isEmpty() && !pickupLocation.equals(edtPickup.getText().toString())) {
            edtPickup.setText(pickupLocation);
            updated = true;
        }

        if (destinationLocation != null && !destinationLocation.isEmpty() && !destinationLocation.equals(edtDestination.getText().toString())) {
            edtDestination.setText(destinationLocation);
            updated = true;
        }

        if (updated) {
            scheduleRoutePreviewUpdate();
        }

        LocationData.clear();
    }

    private String getVehicleLabel() {
        if (VEHICLE_CAR.equals(selectedVehicle)) {
            return "Ô tô";
        }
        return "Xe máy";
    }

    private String getPaymentLabel() {
        if (PAYMENT_BANK_TRANSFER.equals(selectedPayment)) {
            return "Chuyển khoản";
        }
        return "Tiền mặt";
    }

    private void openMapBooking() {
        if (getActivity() instanceof vnuhcm.hcmus.movify.activities.HomeActivity) {
            ((vnuhcm.hcmus.movify.activities.HomeActivity) getActivity()).openMapBooking();
        }
    }

    private void setupPlacesAutocomplete() {
        attachPlacesAutocomplete();
    }
}
