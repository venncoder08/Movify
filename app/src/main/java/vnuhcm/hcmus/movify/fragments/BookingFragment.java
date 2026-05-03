package vnuhcm.hcmus.movify.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vnuhcm.hcmus.movify.R;
import vnuhcm.hcmus.movify.adapters.PromotionAdapter;
import vnuhcm.hcmus.movify.models.AccountProfile;
import vnuhcm.hcmus.movify.models.Promotion;
import vnuhcm.hcmus.movify.services.AuthApiService;
import vnuhcm.hcmus.movify.services.PromotionService;
import vnuhcm.hcmus.movify.utils.LocationData;
import vnuhcm.hcmus.movify.utils.PlacesAutocompleteHelper;

public class BookingFragment extends Fragment {

    private static final String ARG_IS_DRIVER_MODE = "is_driver_mode";
    private static final String TAG = "BookingFragment";
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
    private TextView tvChooseVehicle;
    private TextView tvPaymentTitle;
    private TextView tvPromoTitle;
    private RecyclerView rvPromotions;

    private PromotionAdapter promotionAdapter;
    private ExecutorService promotionExecutor;
    private AuthApiService authApiService;

    private boolean isDriverMode;
    private String selectedPromo = "";
    private String selectedVehicle = VEHICLE_MOTORBIKE;
    private String selectedPayment = PAYMENT_CASH;

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
        authApiService = new AuthApiService(requireContext());
        initViews(view);
        renderHeaderLoading();

        String pickupLocation = LocationData.getPickupLocation();
        String destinationLocation = LocationData.getDestinationLocation();

        loadHeaderProfile();

        if (isDriverMode) {
            applyDriverMode();
        } else {
            setupVehicleButtons();
            setupPaymentButtons();
            setupPromotions();
            setupSearchDriverButton();
            setupPlacesAutocomplete();
        }

        if (!isDriverMode && pickupLocation != null && !pickupLocation.isEmpty()) {
            edtPickup.setText(pickupLocation);
        }
        if (!isDriverMode && destinationLocation != null && !destinationLocation.isEmpty()) {
            edtDestination.setText(destinationLocation);
        }

        LocationData.clear();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mainHandler.removeCallbacksAndMessages(null);
        if (promotionExecutor != null) {
            promotionExecutor.shutdownNow();
            promotionExecutor = null;
        }
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
        });

        btnCar.setOnClickListener(v -> {
            selectedVehicle = VEHICLE_CAR;
            updateVehicleUI();
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
            selectedPromo = promotion.getTitle();
            Toast.makeText(requireContext(), getString(R.string.selected_promo) + selectedPromo, Toast.LENGTH_SHORT).show();
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

    private void setupSearchDriverButton() {
        btnSearchDriver.setOnClickListener(v -> {
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

            Toast.makeText(
                    requireContext(),
                    getString(R.string.finding_driver) + "\n"
                            + "Xe: " + getVehicleLabel()
                            + "\n" + getString(R.string.payment_label) + getPaymentLabel()
                            + (selectedPromo.isEmpty() ? "" : "\nKM: " + selectedPromo),
                    Toast.LENGTH_LONG
            ).show();
        });
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
        new PlacesAutocompleteHelper(requireContext(), edtPickup);
        new PlacesAutocompleteHelper(requireContext(), edtDestination);

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
}
