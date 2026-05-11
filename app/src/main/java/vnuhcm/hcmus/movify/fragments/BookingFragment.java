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
import android.app.Activity;
import android.content.Intent;
import android.view.Gravity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vnuhcm.hcmus.movify.R;
import vnuhcm.hcmus.movify.activities.TripRatingActivity;
import vnuhcm.hcmus.movify.adapters.DestinationRecommendationAdapter;
import vnuhcm.hcmus.movify.adapters.PromotionAdapter;
import vnuhcm.hcmus.movify.models.AccountProfile;
import vnuhcm.hcmus.movify.models.DestinationRecommendation;
import vnuhcm.hcmus.movify.models.PlaceDetails;
import vnuhcm.hcmus.movify.models.Promotion;
import vnuhcm.hcmus.movify.models.RoutePreview;
import vnuhcm.hcmus.movify.models.Trip;
import vnuhcm.hcmus.movify.services.AuthApiService;
import vnuhcm.hcmus.movify.services.PlaceService;
import vnuhcm.hcmus.movify.services.PromotionService;
import vnuhcm.hcmus.movify.services.RouteService;
import vnuhcm.hcmus.movify.utils.CurrentLocationResolver;
import vnuhcm.hcmus.movify.utils.DriverSearchNotificationHelper;
import vnuhcm.hcmus.movify.utils.LocationData;
import vnuhcm.hcmus.movify.utils.LocationHistoryStore;
import vnuhcm.hcmus.movify.utils.PlacesAutocompleteHelper;
import vnuhcm.hcmus.movify.services.TripService;

public class BookingFragment extends Fragment implements OnMapReadyCallback {

    private static final String ARG_IS_DRIVER_MODE = "is_driver_mode";
    private static final String TAG = "BookingFragment";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 201;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 202;
    private static final int REQUEST_QR_SCAN = 301;
    private static final String VEHICLE_MOTORBIKE = "motorbike";
    private static final String VEHICLE_CAR = "car";
    private static final String PAYMENT_CASH = "cash";
    private static final String PAYMENT_BANK_TRANSFER = "bank_transfer";
    private static final long TRIP_STATUS_POLL_INTERVAL_MS = 5000L;
    private static final LatLng HCMC = new LatLng(10.8231, 106.6297);
    private static final int ACTIVE_TRIP_ROUTE_PADDING_PX = 120;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<Promotion> promotionList = new ArrayList<>();
    private final List<DestinationRecommendation> destinationRecommendations = new ArrayList<>();

    private AutoCompleteTextView edtPickup;
    private AutoCompleteTextView edtDestination;
    private AutoCompleteTextView edtNearbyQuery;
    private View headerLayout;
    private LinearLayout bookingContentRoot;
    private LinearLayout locationContainer;
    private LinearLayout pickupContainer;
    private LinearLayout destinationContainer;
    private LinearLayout nearbyRecommendationContainer;

    private LinearLayout vehicleContainer;
    private LinearLayout btnMotorbike;
    private LinearLayout btnCar;
    private LinearLayout btnCash;
    private LinearLayout btnBankTransfer;
    private LinearLayout btnSearchDriver;
    private LinearLayout promoContainer;
    private LinearLayout driverModeContainer;
    private LinearLayout activeTripContainer;
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
    private TextView tvActiveTripStatus;
    private TextView tvActiveTripDriver;
    private TextView tvActiveTripDistance;
    private TextView btnCancelActiveTrip;
    private TextView btnFindNearby;
    private TextView tvNearbyRecommendationStatus;
    private RecyclerView rvPromotions;
    private RecyclerView rvNearbyRecommendations;
    private SupportMapFragment activeTripMapFragment;
    private GoogleMap activeTripMap;

    private PromotionAdapter promotionAdapter;
    private DestinationRecommendationAdapter destinationRecommendationAdapter;
    private ExecutorService promotionExecutor;
    private ExecutorService routeExecutor;
    private AuthApiService authApiService;

    private boolean isDriverMode;
    private RoutePreview currentRoutePreview;
    private Runnable pendingRouteUpdate;
    private AutoCompleteTextView pendingCurrentLocationField;
    private boolean pendingNearbyRecommendationSearch = false;
    private vnuhcm.hcmus.movify.models.Promotion selectedPromotion = null;
    private String selectedVehicle = VEHICLE_MOTORBIKE;
    private String selectedPayment = PAYMENT_CASH;

    // Driver-search countdown
    private CountDownTimer searchCountdownTimer;
    private boolean isSearchingDriver = false;
    private String currentTripId = null;   // backend-assigned id after createTrip succeeds
    private String activeTripId = null;
    private Trip activeTrip = null;
    private final List<LatLng> activeTripRoutePoints = new ArrayList<>();
    private boolean activeTripRouteLoading = false;
    private String activeTripRouteKey = "";
    private Runnable pendingTripStatusPoll;
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
            setupQrScannerButton();
            setupPlacesAutocomplete();
            setupDestinationRecommendations();
            setupActiveTripMap();
            attachRoutePreviewWatchers();
            loadActiveTripForPassenger();
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
        pendingNearbyRecommendationSearch = false;
        activeTripMap = null;
        activeTripMapFragment = null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (pendingCurrentLocationField != null) {
                    resolveAndFillCurrentLocation(pendingCurrentLocationField);
                } else if (pendingNearbyRecommendationSearch) {
                    pendingNearbyRecommendationSearch = false;
                    loadDestinationRecommendations();
                }
            } else {
                Toast.makeText(requireContext(), "Cần quyền vị trí để dùng tùy chọn này", Toast.LENGTH_SHORT).show();
                pendingNearbyRecommendationSearch = false;
            }
            pendingCurrentLocationField = null;
            return;
        }

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // start scanner activity
                Intent intent = new Intent(requireContext(), vnuhcm.hcmus.movify.activities.QrScannerActivity.class);
                startActivityForResult(intent, REQUEST_QR_SCAN);
            } else {
                Toast.makeText(requireContext(), "Cần quyền camera để quét mã", Toast.LENGTH_SHORT).show();
            }
            return;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_QR_SCAN && resultCode == Activity.RESULT_OK && data != null) {
            String value = data.getStringExtra("qr_value");
            if (value != null) {
                // Put scanned value into destination field and update route
                edtDestination.setText(value);
                scheduleRoutePreviewUpdate();
                Toast.makeText(requireContext(), getString(R.string.scanned_value) + ": " + value, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupQrScannerButton() {
        if (ivQr == null) return;
        ivQr.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.CAMERA},
                        CAMERA_PERMISSION_REQUEST_CODE);
                return;
            }

            Intent intent = new Intent(requireContext(), vnuhcm.hcmus.movify.activities.QrScannerActivity.class);
            startActivityForResult(intent, REQUEST_QR_SCAN);
        });
    }

    private void initViews(View view) {
        edtPickup = view.findViewById(R.id.edtPickup);
        edtDestination = view.findViewById(R.id.edtDestination);
        edtNearbyQuery = view.findViewById(R.id.edtNearbyQuery);
        headerLayout = view.findViewById(R.id.headerLayout);
        bookingContentRoot = view.findViewById(R.id.bookingContentRoot);
        locationContainer = view.findViewById(R.id.locationContainer);
        pickupContainer = view.findViewById(R.id.pickupContainer);
        destinationContainer = view.findViewById(R.id.destinationContainer);
        nearbyRecommendationContainer = view.findViewById(R.id.nearbyRecommendationContainer);

        vehicleContainer = view.findViewById(R.id.vehicleContainer);
        btnMotorbike = view.findViewById(R.id.btnMotorbike);
        btnCar = view.findViewById(R.id.btnCar);
        btnCash = view.findViewById(R.id.btnCash);
        btnBankTransfer = view.findViewById(R.id.btnBankTransfer);
        btnSearchDriver = view.findViewById(R.id.btnSearchDriver);
        promoContainer = view.findViewById(R.id.promoContainer);
        driverModeContainer = view.findViewById(R.id.driverModeContainer);
        activeTripContainer = view.findViewById(R.id.activeTripContainer);

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
        tvActiveTripStatus = view.findViewById(R.id.tvActiveTripStatus);
        tvActiveTripDriver = view.findViewById(R.id.tvActiveTripDriver);
        tvActiveTripDistance = view.findViewById(R.id.tvActiveTripDistance);
        btnCancelActiveTrip = view.findViewById(R.id.btnCancelActiveTrip);
        btnFindNearby = view.findViewById(R.id.btnFindNearby);
        tvNearbyRecommendationStatus = view.findViewById(R.id.tvNearbyRecommendationStatus);

        rvPromotions = view.findViewById(R.id.rvPromotions);
        rvNearbyRecommendations = view.findViewById(R.id.rvNearbyRecommendations);
    }

    private void setupActiveTripMap() {
        activeTripMapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.mapActiveTrip);
        if (activeTripMapFragment != null) {
            activeTripMapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        activeTripMap = googleMap;
        activeTripMap.moveCamera(CameraUpdateFactory.newLatLngZoom(HCMC, 12f));
        renderActiveTripMap(activeTrip);
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
        setVisible(nearbyRecommendationContainer, false);
        setVisible(tvChooseVehicle, false);
        setVisible(vehicleContainer, false);
        setVisible(tvPaymentTitle, false);
        setVisible(btnCash, false);
        setVisible(btnBankTransfer, false);
        setVisible(tvPromoTitle, false);
        setVisible(promoContainer, false);
        setVisible(btnSearchDriver, false);
        setVisible(routeSummaryContainer, false);
        setVisible(activeTripContainer, false);
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

    private void setupDestinationRecommendations() {
        destinationRecommendationAdapter = new DestinationRecommendationAdapter(
                destinationRecommendations,
                this::applyDestinationRecommendation
        );
        rvNearbyRecommendations.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvNearbyRecommendations.setAdapter(destinationRecommendationAdapter);
        rvNearbyRecommendations.setNestedScrollingEnabled(false);

        btnFindNearby.setOnClickListener(v -> loadDestinationRecommendations());
    }

    private void loadDestinationRecommendations() {
        if (routeExecutor == null || routeExecutor.isShutdown() || !isAdded()) {
            return;
        }

        final String query = edtNearbyQuery.getText().toString().trim();
        if (TextUtils.isEmpty(query)) {
            Toast.makeText(requireContext(), R.string.nearby_recommendation_need_query, Toast.LENGTH_SHORT).show();
            return;
        }

        final String pickupText = edtPickup.getText().toString().trim();
        if (TextUtils.isEmpty(pickupText)
                && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            pendingNearbyRecommendationSearch = true;
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        setNearbyRecommendationLoading(true);
        Context appContext = requireContext().getApplicationContext();
        routeExecutor.execute(() -> {
            try {
                String resolvedOriginLabel = null;
                PlaceDetails originDetails;
                if (TextUtils.isEmpty(pickupText)) {
                    resolvedOriginLabel = CurrentLocationResolver.resolveCurrentLocationLabel(appContext);
                    originDetails = PlaceService.fetchPlaceDetails(appContext, resolvedOriginLabel);
                } else {
                    originDetails = PlaceService.fetchPlaceDetails(appContext, pickupText);
                }

                List<DestinationRecommendation> recommendations =
                        PlaceService.fetchDestinationRecommendations(appContext, query, originDetails.latLng);
                String finalResolvedOriginLabel = resolvedOriginLabel;
                mainHandler.post(() -> {
                    if (!isAdded()) {
                        return;
                    }

                    if (!TextUtils.isEmpty(finalResolvedOriginLabel)
                            && TextUtils.isEmpty(edtPickup.getText().toString().trim())) {
                        edtPickup.setText(finalResolvedOriginLabel);
                    }
                    renderDestinationRecommendations(recommendations);
                });
            } catch (Exception exception) {
                Log.w(TAG, "Unable to load destination recommendations", exception);
                mainHandler.post(() -> {
                    if (!isAdded()) {
                        return;
                    }

                    setNearbyRecommendationLoading(false);
                    setNearbyRecommendationStatus(getString(R.string.nearby_recommendation_origin_error)
                            + ": "
                            + exception.getMessage());
                    Toast.makeText(requireContext(), R.string.nearby_recommendation_origin_error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void renderDestinationRecommendations(@NonNull List<DestinationRecommendation> recommendations) {
        setNearbyRecommendationLoading(false);
        destinationRecommendations.clear();
        destinationRecommendations.addAll(recommendations);
        destinationRecommendationAdapter.notifyDataSetChanged();

        boolean hasRecommendations = !destinationRecommendations.isEmpty();
        setVisible(rvNearbyRecommendations, hasRecommendations);
        if (hasRecommendations) {
            setVisible(tvNearbyRecommendationStatus, false);
        } else {
            setNearbyRecommendationStatus(getString(R.string.nearby_recommendation_empty));
        }
    }

    private void applyDestinationRecommendation(@NonNull DestinationRecommendation recommendation) {
        edtDestination.setText(recommendation.getRouteLabel());
        saveLocationHistoryForField(edtDestination, recommendation.getLabel());
        scheduleRoutePreviewUpdate();
        setNearbyRecommendationStatus(recommendation.getReason());
        Toast.makeText(requireContext(), R.string.nearby_recommendation_selected, Toast.LENGTH_SHORT).show();
    }

    private void setNearbyRecommendationLoading(boolean loading) {
        btnFindNearby.setEnabled(!loading);
        btnFindNearby.setText(R.string.nearby_recommendation_search);
        setVisible(rvNearbyRecommendations, false);
        if (loading) {
            setNearbyRecommendationStatus(getString(R.string.nearby_recommendation_loading));
        }
    }

    private void setNearbyRecommendationStatus(@NonNull String message) {
        tvNearbyRecommendationStatus.setText(message);
        setVisible(tvNearbyRecommendationStatus, true);
    }

    private void attachPlacesAutocomplete() {
        new PlacesAutocompleteHelper(
                requireContext(),
                edtPickup,
                this::handleCurrentLocationSelection,
                LocationHistoryStore.KEY_PICKUP
        );
        new PlacesAutocompleteHelper(
                requireContext(),
                edtDestination,
                this::handleCurrentLocationSelection,
                LocationHistoryStore.KEY_DESTINATION
        );

        pickupContainer.setOnClickListener(v -> openMapBooking());
        destinationContainer.setOnClickListener(v -> openMapBooking());
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
                    saveLocationHistoryForField(
                            targetField,
                            CurrentLocationResolver.getShareableLocationLabel(currentLocationLabel)
                    );
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
        btnCancelActiveTrip.setOnClickListener(v -> showCancelActiveTripDialog());
        btnSearchDriver.setOnClickListener(v -> {
            if (isSearchingDriver) {
                stopSearchCountdown(true);
                return;
            }

            if (activeTrip != null) {
                handleActiveTripAction();
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
        final String shareablePickup = CurrentLocationResolver.getShareableLocationLabel(pickup);
        final String shareableDestination = CurrentLocationResolver.getShareableLocationLabel(destination);

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
                        shareablePickup, fPickupLat, fPickupLng,
                        shareableDestination, fDestLat, fDestLng,
                        selectedVehicle, selectedPayment,
                        promoId, finalFare, finalDist
                );
                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    saveLocationHistory(shareablePickup, shareableDestination);
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
        scheduleTripStatusPoll();
    }

    private void stopSearchCountdown(boolean userCancelled) {
        stopSearchCountdown(userCancelled, true);
    }

    private void stopSearchCountdown(boolean userCancelled, boolean cancelBackendTrip) {
        isSearchingDriver = false;
        if (pendingTripStatusPoll != null) {
            mainHandler.removeCallbacks(pendingTripStatusPoll);
            pendingTripStatusPoll = null;
        }
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
        if (cancelBackendTrip && tripId != null && getContext() != null) {
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

    private void loadActiveTripForPassenger() {
        if (routeExecutor == null || routeExecutor.isShutdown()) {
            return;
        }

        final Context appContext = requireContext().getApplicationContext();
        routeExecutor.execute(() -> {
            try {
                Trip trip = TripService.fetchActiveTrip(appContext);
                mainHandler.post(() -> {
                    if (!isAdded() || trip == null) {
                        return;
                    }

                    if ("Pending".equalsIgnoreCase(trip.getStatus())) {
                        currentTripId = trip.getId();
                        activeTripId = null;
                        activeTrip = null;
                        isSearchingDriver = true;
                        btnSearchDriver.setEnabled(true);
        setSearchButtonSearching("Đang tìm tài xế...");
                        scheduleTripStatusPoll();
                        return;
                    }

                    handlePolledTripStatus(trip);
                });
            } catch (Exception exception) {
                Log.w(TAG, "Unable to load active trip", exception);
            }
        });
    }

    private void setActiveTripMode(boolean active) {
        setVisible(activeTripContainer, active);
        setVisible(btnCancelActiveTrip, false);
        setVisible(locationContainer, !active);
        setVisible(routeSummaryContainer, !active && currentRoutePreview != null);
        setVisible(tvChooseVehicle, !active);
        setVisible(vehicleContainer, !active);
        setVisible(tvPaymentTitle, !active);
        setVisible(btnCash, !active);
        setVisible(btnBankTransfer, !active);
        setVisible(tvPromoTitle, !active);
        setVisible(promoContainer, !active);
        setVisible(btnSearchDriver, true);
    }

    private void renderPassengerActiveTrip(@NonNull Trip trip) {
        activeTrip = trip;
        activeTripId = trip.getId();
        setActiveTripMode(true);

        boolean inProgress = "InProgress".equalsIgnoreCase(trip.getStatus());
        boolean canCancel = !inProgress && "Accepted".equalsIgnoreCase(trip.getStatus());
        setVisible(btnCancelActiveTrip, canCancel);
        btnCancelActiveTrip.setEnabled(canCancel);
        tvActiveTripStatus.setText(inProgress
                ? "Chuyến đi đang diễn ra"
                : "Tài xế đang đến điểm đón");

        String driverName = trip.getDriverFullName();
        String driverPhone = trip.getDriverPhoneNumber();
        if (TextUtils.isEmpty(driverName)) {
            tvActiveTripDriver.setText("Tài xế đã nhận chuyến");
        } else if (TextUtils.isEmpty(driverPhone)) {
            tvActiveTripDriver.setText("Tài xế: " + driverName);
        } else {
            tvActiveTripDriver.setText("Tài xế: " + driverName + " - " + driverPhone);
        }

        LatLng driverLocation = getDriverLatLng(trip);
        LatLng targetLocation = inProgress ? getDestinationLatLng(trip) : getPickupLatLng(trip);
        if (driverLocation != null && targetLocation != null) {
            double distanceMeters = calculateDistanceMeters(driverLocation, targetLocation);
            tvActiveTripDistance.setText((inProgress ? "Còn " : "Cách điểm đón ")
                    + formatMeters(distanceMeters));
        } else {
            tvActiveTripDistance.setText("Đang chờ cập nhật vị trí tài xế");
        }

        renderActiveTripMap(trip);
        ensureActiveTripRoute(trip);
    }

    private void ensureActiveTripRoute(@Nullable Trip trip) {
        if (trip == null || routeExecutor == null || routeExecutor.isShutdown()) {
            return;
        }

        boolean inProgress = "InProgress".equalsIgnoreCase(trip.getStatus());
        LatLng start = inProgress ? getPickupLatLng(trip) : getDriverLatLng(trip);
        LatLng end = inProgress ? getDestinationLatLng(trip) : getPickupLatLng(trip);
        if (start == null || end == null) {
            return;
        }

        String key = trip.getId() + ":" + (inProgress ? "destination" : "pickup");
        if (activeTripRouteLoading || key.equals(activeTripRouteKey)) {
            return;
        }

        activeTripRouteLoading = true;
        activeTripRoutePoints.clear();
        renderActiveTripMap(trip);
        final Context appContext = requireContext().getApplicationContext();
        routeExecutor.execute(() -> {
            try {
                RoutePreview routePreview = RouteService.fetchRoutePreview(
                        appContext,
                        formatLatLng(start),
                        formatLatLng(end)
                );
                List<LatLng> routePoints = new ArrayList<>(routePreview.routePoints);
                mainHandler.post(() -> {
                    if (!isAdded()) {
                        activeTripRouteLoading = false;
                        return;
                    }
                    activeTripRouteLoading = false;
                    activeTripRouteKey = key;
                    activeTripRoutePoints.clear();
                    activeTripRoutePoints.addAll(routePoints);
                    renderActiveTripMap(activeTrip);
                });
            } catch (Exception exception) {
                Log.w(TAG, "Unable to draw passenger active route", exception);
                mainHandler.post(() -> activeTripRouteLoading = false);
            }
        });
    }

    private void scheduleTripStatusPoll() {
        String tripId = getTripIdForStatusPoll();
        if (tripId == null) {
            return;
        }

        if (pendingTripStatusPoll != null) {
            mainHandler.removeCallbacks(pendingTripStatusPoll);
        }

        pendingTripStatusPoll = this::pollTripStatus;
        mainHandler.postDelayed(pendingTripStatusPoll, TRIP_STATUS_POLL_INTERVAL_MS);
    }

    private void pollTripStatus() {
        pendingTripStatusPoll = null;
        final String tripId = getTripIdForStatusPoll();
        if (tripId == null || routeExecutor == null || routeExecutor.isShutdown()) {
            return;
        }

        final Context appContext = requireContext().getApplicationContext();
        routeExecutor.execute(() -> {
            try {
                Trip trip = TripService.fetchTrip(appContext, tripId);
                mainHandler.post(() -> handlePolledTripStatus(trip));
            } catch (Exception exception) {
                Log.w(TAG, "Unable to poll trip status", exception);
                mainHandler.post(this::scheduleTripStatusPoll);
            }
        });
    }

    private void handlePolledTripStatus(@NonNull Trip trip) {
        if (!isAdded()) {
            return;
        }

        String status = trip.getStatus();
        if ("Accepted".equalsIgnoreCase(status)) {
            boolean wasSearching = isSearchingDriver;
            activeTripId = trip.getId();
            activeTrip = trip;
            if (wasSearching) {
                stopSearchCountdown(false, false);
                DriverSearchNotificationHelper.notifyDriverAccepted(requireContext().getApplicationContext());
                Toast.makeText(requireContext(), "Tài xế đã nhận chuyến và đang đến điểm đón", Toast.LENGTH_LONG).show();
            }
            renderPassengerActiveTrip(trip);
            renderAcceptedTripState(trip);
            scheduleTripStatusPoll();
            return;
        }

        if ("InProgress".equalsIgnoreCase(status)) {
            boolean wasAlreadyInProgress = activeTrip != null
                    && "InProgress".equalsIgnoreCase(activeTrip.getStatus());
            activeTripId = trip.getId();
            activeTrip = trip;
            if (isSearchingDriver) {
                stopSearchCountdown(false, false);
            }
            renderPassengerActiveTrip(trip);
            setSearchButtonSearching("Chuyến đi đang diễn ra");
            btnSearchDriver.setEnabled(false);
            if (!wasAlreadyInProgress) {
                Toast.makeText(requireContext(), "Chuyến đi đã bắt đầu", Toast.LENGTH_SHORT).show();
            }
            scheduleTripStatusPoll();
            return;
        }

        if ("Cancelled".equalsIgnoreCase(status) || "Completed".equalsIgnoreCase(status)) {
            if (pendingTripStatusPoll != null) {
                mainHandler.removeCallbacks(pendingTripStatusPoll);
                pendingTripStatusPoll = null;
            }
            stopSearchCountdown(false, false);
            btnSearchDriver.setEnabled(true);
            setSearchButtonLabel(R.string.find_driver);
            Toast.makeText(requireContext(),
                    "Completed".equalsIgnoreCase(status)
                            ? getString(R.string.trip_completed_rating_toast)
                            : "Chuyến đi đã chuyển sang trạng thái " + mapTripStatusForDisplay(status),
                    Toast.LENGTH_LONG).show();
            currentTripId = null;
            activeTripId = null;
            activeTrip = null;
            activeTripRouteKey = "";
            activeTripRoutePoints.clear();
            setActiveTripMode(false);
            if ("Completed".equalsIgnoreCase(status)) {
                openTripRatingScreen(trip);
            }
            return;
        }

        scheduleTripStatusPoll();
    }

    @Nullable
    private String getTripIdForStatusPoll() {
        if (isSearchingDriver && currentTripId != null) {
            return currentTripId;
        }

        return activeTripId;
    }

    private String mapTripStatusForDisplay(String status) {
        if ("Pending".equalsIgnoreCase(status)) {
            return "đang chờ";
        }

        if ("Accepted".equalsIgnoreCase(status)) {
            return "đã nhận";
        }

        if ("InProgress".equalsIgnoreCase(status)) {
            return "đang diễn ra";
        }

        if ("Completed".equalsIgnoreCase(status)) {
            return "đã hoàn thành";
        }

        if ("Cancelled".equalsIgnoreCase(status)) {
            return "đã hủy";
        }

        return status;
    }

    private void openTripRatingScreen(@NonNull Trip trip) {
        if (!isAdded() || TextUtils.isEmpty(trip.getId())) {
            return;
        }

        Intent intent = new Intent(requireContext(), TripRatingActivity.class);
        intent.putExtra(TripRatingActivity.EXTRA_TRIP_ID, trip.getId());
        intent.putExtra(TripRatingActivity.EXTRA_DRIVER_NAME, trip.getDriverFullName());
        startActivity(intent);
    }

    private void renderAcceptedTripState(@NonNull Trip trip) {
        activeTrip = trip;
        activeTripId = trip.getId();

        if (trip.isPassengerPickupConfirmed()) {
            setSearchButtonSearching("Đã xác nhận, chờ bắt đầu chuyến");
            btnSearchDriver.setEnabled(false);
            return;
        }

        if (trip.isDriverPickupConfirmed()) {
            setSearchButtonSearching("Xác nhận đã lên xe");
            btnSearchDriver.setEnabled(true);
            return;
        }

        setSearchButtonSearching("Chờ tài xế đến điểm đón");
        btnSearchDriver.setEnabled(false);
    }

    private void handleActiveTripAction() {
        if (activeTrip == null) {
            return;
        }

        if (!"Accepted".equalsIgnoreCase(activeTrip.getStatus())) {
            Toast.makeText(requireContext(), "Chuyến đi đang ở trạng thái " + activeTrip.getStatus(), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!activeTrip.isDriverPickupConfirmed()) {
            Toast.makeText(requireContext(), "Tài xế chưa xác nhận đã đến điểm đón", Toast.LENGTH_SHORT).show();
            return;
        }

        if (activeTrip.isPassengerPickupConfirmed()) {
            Toast.makeText(requireContext(), "Bạn đã xác nhận lên xe", Toast.LENGTH_SHORT).show();
            return;
        }

        confirmPassengerPickup();
    }

    private void showCancelActiveTripDialog() {
        if (activeTrip == null || activeTripId == null) {
            return;
        }

        if ("InProgress".equalsIgnoreCase(activeTrip.getStatus())) {
            Toast.makeText(requireContext(), "Chuyến đi đã bắt đầu nên không thể hủy", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Hủy chuyến")
                .setMessage("Bạn có chắc muốn hủy chuyến này không?")
                .setNegativeButton("Ở lại", null)
                .setPositiveButton("Hủy chuyến", (dialog, which) -> cancelActiveTrip())
                .show();
    }

    private void cancelActiveTrip() {
        if (activeTripId == null || routeExecutor == null || routeExecutor.isShutdown()) {
            return;
        }

        final String tripId = activeTripId;
        final Context appContext = requireContext().getApplicationContext();
        if (pendingTripStatusPoll != null) {
            mainHandler.removeCallbacks(pendingTripStatusPoll);
            pendingTripStatusPoll = null;
        }
        btnCancelActiveTrip.setEnabled(false);
        btnSearchDriver.setEnabled(false);
        setSearchButtonSearching("Đang hủy chuyến...");

        routeExecutor.execute(() -> {
            try {
                TripService.cancelTrip(appContext, tripId);
                mainHandler.post(() -> {
                    if (!isAdded()) {
                        return;
                    }

                    Toast.makeText(requireContext(), "Đã hủy chuyến", Toast.LENGTH_SHORT).show();
                    currentTripId = null;
                    activeTripId = null;
                    activeTrip = null;
                    activeTripRouteKey = "";
                    activeTripRoutePoints.clear();
                    activeTripRouteLoading = false;
                    btnCancelActiveTrip.setEnabled(true);
                    btnSearchDriver.setEnabled(true);
                    setSearchButtonLabel(R.string.find_driver);
                    setActiveTripMode(false);
                });
            } catch (Exception exception) {
                Log.w(TAG, "Unable to cancel active trip", exception);
                mainHandler.post(() -> {
                    if (!isAdded()) {
                        return;
                    }

                    btnCancelActiveTrip.setEnabled(true);
                    if (activeTrip != null) {
                        renderPassengerActiveTrip(activeTrip);
                        renderAcceptedTripState(activeTrip);
                        scheduleTripStatusPoll();
                    } else {
                        btnSearchDriver.setEnabled(true);
                        setSearchButtonLabel(R.string.find_driver);
                    }
                    Toast.makeText(requireContext(), "Không thể hủy chuyến: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void confirmPassengerPickup() {
        if (activeTripId == null || routeExecutor == null || routeExecutor.isShutdown()) {
            return;
        }

        btnSearchDriver.setEnabled(false);
        setSearchButtonSearching("Đang xác nhận...");
        final String tripId = activeTripId;
        final Context appContext = requireContext().getApplicationContext();
        routeExecutor.execute(() -> {
            try {
                Trip updatedTrip = TripService.confirmPickup(appContext, tripId, null, null);
                mainHandler.post(() -> {
                    if (!isAdded()) {
                        return;
                    }

                    Toast.makeText(requireContext(), "Đã xác nhận lên xe", Toast.LENGTH_SHORT).show();
                    handlePolledTripStatus(updatedTrip);
                });
            } catch (Exception exception) {
                Log.w(TAG, "Unable to confirm passenger pickup", exception);
                mainHandler.post(() -> {
                    if (!isAdded()) {
                        return;
                    }

                    if (activeTrip != null) {
                        renderAcceptedTripState(activeTrip);
                    }
                    Toast.makeText(requireContext(), "Không thể xác nhận: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void renderActiveTripMap(@Nullable Trip trip) {
        if (activeTripMap == null || trip == null) {
            return;
        }

        activeTripMap.clear();
        LatLng driverLocation = getDriverLatLng(trip);
        LatLng pickupLocation = getPickupLatLng(trip);
        LatLng destinationLocation = getDestinationLatLng(trip);
        boolean inProgress = "InProgress".equalsIgnoreCase(trip.getStatus());

        if (driverLocation != null) {
            activeTripMap.addMarker(new MarkerOptions()
                    .position(driverLocation)
                    .title("Vị trí tài xế")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        }

        if (!inProgress && pickupLocation != null) {
            activeTripMap.addMarker(new MarkerOptions()
                    .position(pickupLocation)
                    .title("Điểm đón")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        }

        if (destinationLocation != null) {
            activeTripMap.addMarker(new MarkerOptions()
                    .position(destinationLocation)
                    .title("Điểm đến")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        }

        if (!activeTripRoutePoints.isEmpty()) {
            activeTripMap.addPolyline(new PolylineOptions()
                    .addAll(activeTripRoutePoints)
                    .color(ContextCompat.getColor(requireContext(), R.color.primary))
                    .width(10f)
                    .geodesic(true));
        }

        fitActiveTripBounds(driverLocation, inProgress ? destinationLocation : pickupLocation);
    }

    private void fitActiveTripBounds(@Nullable LatLng first, @Nullable LatLng second) {
        if (activeTripMap == null) {
            return;
        }

        LatLngBounds.Builder builder = LatLngBounds.builder();
        int included = 0;
        if (first != null) {
            builder.include(first);
            included++;
        }
        if (second != null) {
            builder.include(second);
            included++;
        }
        for (LatLng routePoint : activeTripRoutePoints) {
            if (routePoint != null) {
                builder.include(routePoint);
                included++;
            }
        }

        if (included >= 2) {
            activeTripMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), ACTIVE_TRIP_ROUTE_PADDING_PX));
        } else if (included == 1) {
            LatLng focus = first != null ? first : second;
            if (focus != null) {
                activeTripMap.animateCamera(CameraUpdateFactory.newLatLngZoom(focus, 14f));
            }
        }
    }

    @Nullable
    private LatLng getPickupLatLng(@NonNull Trip trip) {
        return isValidCoordinate(trip.getPickupLat(), trip.getPickupLng())
                ? new LatLng(trip.getPickupLat(), trip.getPickupLng())
                : null;
    }

    @Nullable
    private LatLng getDestinationLatLng(@NonNull Trip trip) {
        return isValidCoordinate(trip.getDestinationLat(), trip.getDestinationLng())
                ? new LatLng(trip.getDestinationLat(), trip.getDestinationLng())
                : null;
    }

    @Nullable
    private LatLng getDriverLatLng(@NonNull Trip trip) {
        return isValidCoordinate(trip.getDriverCurrentLat(), trip.getDriverCurrentLng())
                ? new LatLng(trip.getDriverCurrentLat(), trip.getDriverCurrentLng())
                : null;
    }

    private boolean isValidCoordinate(double lat, double lng) {
        return lat >= -90d && lat <= 90d
                && lng >= -180d && lng <= 180d
                && !(Math.abs(lat) < 0.000001d && Math.abs(lng) < 0.000001d);
    }

    private double calculateDistanceMeters(@NonNull LatLng start, @NonNull LatLng end) {
        double earthRadiusMeters = 6371000d;
        double dLat = Math.toRadians(end.latitude - start.latitude);
        double dLng = Math.toRadians(end.longitude - start.longitude);
        double startLat = Math.toRadians(start.latitude);
        double endLat = Math.toRadians(end.latitude);
        double a = Math.sin(dLat / 2d) * Math.sin(dLat / 2d)
                + Math.cos(startLat) * Math.cos(endLat)
                * Math.sin(dLng / 2d) * Math.sin(dLng / 2d);
        double c = 2d * Math.atan2(Math.sqrt(a), Math.sqrt(1d - a));
        return earthRadiusMeters * c;
    }

    @NonNull
    private String formatMeters(double distanceMeters) {
        if (distanceMeters >= 1000d) {
            return String.format(Locale.getDefault(), "%.1f km", distanceMeters / 1000d);
        }

        return Math.max(1, Math.round(distanceMeters)) + " m";
    }

    @NonNull
    private String formatLatLng(@NonNull LatLng latLng) {
        return String.format(Locale.US, "%.6f,%.6f", latLng.latitude, latLng.longitude);
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
            LocationHistoryStore.saveLocation(requireContext(), LocationHistoryStore.KEY_PICKUP, pickupLocation);
            updated = true;
        }

        if (destinationLocation != null && !destinationLocation.isEmpty() && !destinationLocation.equals(edtDestination.getText().toString())) {
            edtDestination.setText(destinationLocation);
            LocationHistoryStore.saveLocation(requireContext(), LocationHistoryStore.KEY_DESTINATION, destinationLocation);
            updated = true;
        }

        if (updated) {
            scheduleRoutePreviewUpdate();
        }

        LocationData.clear();
        loadActiveTripForPassenger();
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

    private void saveLocationHistory(String pickup, String destination) {
        if (!isAdded()) {
            return;
        }

        LocationHistoryStore.saveLocation(requireContext(), LocationHistoryStore.KEY_PICKUP, pickup);
        LocationHistoryStore.saveLocation(requireContext(), LocationHistoryStore.KEY_DESTINATION, destination);
    }

    private void saveLocationHistoryForField(@NonNull AutoCompleteTextView targetField, String location) {
        if (!isAdded()) {
            return;
        }

        if (targetField == edtPickup) {
            LocationHistoryStore.saveLocation(requireContext(), LocationHistoryStore.KEY_PICKUP, location);
            return;
        }

        if (targetField == edtDestination) {
            LocationHistoryStore.saveLocation(requireContext(), LocationHistoryStore.KEY_DESTINATION, location);
        }
    }

    private void setupPlacesAutocomplete() {
        attachPlacesAutocomplete();
    }
}
