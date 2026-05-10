package vnuhcm.hcmus.movify.activities.profile;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import vnuhcm.hcmus.movify.R;
import vnuhcm.hcmus.movify.models.AccountProfile;
import vnuhcm.hcmus.movify.models.DriverRequestStatus;
import vnuhcm.hcmus.movify.services.AuthApiService;
import vnuhcm.hcmus.movify.services.DriverRequestService;
import vnuhcm.hcmus.movify.services.UploadService;
import vnuhcm.hcmus.movify.utils.RemoteImageLoader;

public class BecomeDriverActivity extends AppCompatActivity {

    private static final int DOCUMENT_ID_CARD = 1;
    private static final int DOCUMENT_DRIVER_LICENSE = 2;
    private static final int DOCUMENT_VEHICLE_REGISTRATION = 3;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ActivityResultLauncher<PickVisualMediaRequest> pickDocumentLauncher =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    applyPickedDocument(uri);
                }
            });

    private ExecutorService executorService;
    private AuthApiService authApiService;
    private UploadService uploadService;

    private TextView tvStatusValue;
    private TextView tvStatusNote;
    private ImageView ivIdCardPreview;
    private ImageView ivDriverLicensePreview;
    private ImageView ivVehicleRegistrationPreview;
    private TextView btnVehicleMotor;
    private TextView btnVehicleCar;
    private TextView btnPickIdCardImage;
    private TextView btnPickDriverLicenseImage;
    private TextView btnPickVehicleRegistrationImage;
    private TextView btnSubmitRequest;

    private AccountProfile latestProfile;
    private DriverRequestStatus latestDriverRequest;
    private int pendingDocumentType = DOCUMENT_ID_CARD;

    private Uri selectedIdCardUri;
    private Uri selectedDriverLicenseUri;
    private Uri selectedVehicleRegistrationUri;
    private String idCardImageUrl = "";
    private String driverLicenseImageUrl = "";
    private String vehicleRegistrationImageUrl = "";
    private String selectedVehicleType = "motorbike";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_become_driver);

        authApiService = new AuthApiService(this);
        uploadService = new UploadService(this);
        executorService = Executors.newSingleThreadExecutor();

        initViews();
        loadDriverRequestState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mainHandler.removeCallbacksAndMessages(null);
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }
    }

    private void initViews() {
        ImageView ivBack = findViewById(R.id.ivBack);
        tvStatusValue = findViewById(R.id.tvStatusValue);
        tvStatusNote = findViewById(R.id.tvStatusNote);
        ivIdCardPreview = findViewById(R.id.ivIdCardPreview);
        ivDriverLicensePreview = findViewById(R.id.ivDriverLicensePreview);
        ivVehicleRegistrationPreview = findViewById(R.id.ivVehicleRegistrationPreview);
        btnVehicleMotor = findViewById(R.id.btnVehicleMotor);
        btnVehicleCar = findViewById(R.id.btnVehicleCar);
        btnPickIdCardImage = findViewById(R.id.btnPickIdCardImage);
        btnPickDriverLicenseImage = findViewById(R.id.btnPickDriverLicenseImage);
        btnPickVehicleRegistrationImage = findViewById(R.id.btnPickVehicleRegistrationImage);
        btnSubmitRequest = findViewById(R.id.btnSubmitRequest);

        ivBack.setOnClickListener(v -> finish());
        btnVehicleMotor.setOnClickListener(v -> selectVehicleType("motorbike"));
        btnVehicleCar.setOnClickListener(v -> selectVehicleType("car"));
        btnPickIdCardImage.setOnClickListener(v -> openDocumentPicker(DOCUMENT_ID_CARD));
        btnPickDriverLicenseImage.setOnClickListener(v -> openDocumentPicker(DOCUMENT_DRIVER_LICENSE));
        btnPickVehicleRegistrationImage.setOnClickListener(v -> openDocumentPicker(DOCUMENT_VEHICLE_REGISTRATION));
        btnSubmitRequest.setOnClickListener(v -> submitDriverRequest());
    }

    private void loadDriverRequestState() {
        if (executorService == null || executorService.isShutdown()) {
            return;
        }

        executorService.execute(() -> {
            try {
                AccountProfile profile = authApiService.fetchCurrentUserProfile();
                DriverRequestStatus requestStatus = DriverRequestService.fetchMyDriverRequest(this);
                mainHandler.post(() -> bindState(profile, requestStatus));
            } catch (Exception exception) {
                mainHandler.post(() -> renderError(exception, getString(R.string.driver_request_load_error)));
            }
        });
    }

    private void bindState(AccountProfile profile, DriverRequestStatus requestStatus) {
        latestProfile = profile;
        latestDriverRequest = requestStatus;

        idCardImageUrl = firstNonEmpty(
                requestStatus == null ? "" : requestStatus.getIdCard(),
                profile == null ? "" : profile.getIdCard()
        );
        driverLicenseImageUrl = firstNonEmpty(
                requestStatus == null ? "" : requestStatus.getDriverLicense(),
                profile == null ? "" : profile.getDriverLicense()
        );
        vehicleRegistrationImageUrl = firstNonEmpty(
                requestStatus == null ? "" : requestStatus.getVehicleRegistration(),
                profile == null ? "" : profile.getVehicleRegistration()
        );
        selectedVehicleType = normalizeVehicleType(firstNonEmpty(
                requestStatus == null ? "" : requestStatus.getVehicleType(),
                profile == null ? "" : profile.getVehicleType()
        ));

        selectedIdCardUri = null;
        selectedDriverLicenseUri = null;
        selectedVehicleRegistrationUri = null;

        updateDocumentPreview(ivIdCardPreview, null, idCardImageUrl);
        updateDocumentPreview(ivDriverLicensePreview, null, driverLicenseImageUrl);
        updateDocumentPreview(ivVehicleRegistrationPreview, null, vehicleRegistrationImageUrl);
        renderVehicleTypeSelection();

        String effectiveStatus = firstNonEmpty(
                requestStatus == null ? "" : requestStatus.getLatestRequestStatus(),
                requestStatus == null ? "" : requestStatus.getDriverStatus(),
                profile == null ? "" : profile.getDriverStatus()
        );
        String role = profile == null ? "" : profile.getRole();
        tvStatusValue.setText(mapDriverStatus(role, effectiveStatus));
        tvStatusNote.setText(buildStatusNote(requestStatus));
        updateFormState(role, effectiveStatus);
    }

    private void openDocumentPicker(int documentType) {
        pendingDocumentType = documentType;
        pickDocumentLauncher.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private void applyPickedDocument(Uri uri) {
        switch (pendingDocumentType) {
            case DOCUMENT_DRIVER_LICENSE:
                selectedDriverLicenseUri = uri;
                driverLicenseImageUrl = "";
                updateDocumentPreview(ivDriverLicensePreview, uri, "");
                break;
            case DOCUMENT_VEHICLE_REGISTRATION:
                selectedVehicleRegistrationUri = uri;
                vehicleRegistrationImageUrl = "";
                updateDocumentPreview(ivVehicleRegistrationPreview, uri, "");
                break;
            case DOCUMENT_ID_CARD:
            default:
                selectedIdCardUri = uri;
                idCardImageUrl = "";
                updateDocumentPreview(ivIdCardPreview, uri, "");
                break;
        }
    }

    private void submitDriverRequest() {
        if (!hasDocument(selectedIdCardUri, idCardImageUrl)
                || !hasDocument(selectedDriverLicenseUri, driverLicenseImageUrl)
                || !hasDocument(selectedVehicleRegistrationUri, vehicleRegistrationImageUrl)) {
            Toast.makeText(this, R.string.driver_request_missing_images, Toast.LENGTH_SHORT).show();
            return;
        }

        if (executorService == null || executorService.isShutdown()) {
            return;
        }

        setSubmitting(true);
        executorService.execute(() -> {
            try {
                String jwtToken = authApiService.getJwtToken();
                if (TextUtils.isEmpty(jwtToken)) {
                    throw new IOException("Không tìm thấy mã phiên đăng nhập");
                }

                String uploadedIdCard = uploadDocumentIfNeeded(selectedIdCardUri, idCardImageUrl, jwtToken);
                String uploadedDriverLicense = uploadDocumentIfNeeded(selectedDriverLicenseUri, driverLicenseImageUrl, jwtToken);
                String uploadedVehicleRegistration = uploadDocumentIfNeeded(selectedVehicleRegistrationUri, vehicleRegistrationImageUrl, jwtToken);

                String message = DriverRequestService.submitDriverRequest(
                        this,
                        uploadedIdCard,
                        uploadedDriverLicense,
                        uploadedVehicleRegistration,
                        selectedVehicleType
                );
                AccountProfile profile = authApiService.fetchCurrentUserProfile();
                DriverRequestStatus requestStatus = DriverRequestService.fetchMyDriverRequest(this);
                mainHandler.post(() -> {
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    setSubmitting(false);
                    bindState(profile, requestStatus);
                });
            } catch (Exception exception) {
                mainHandler.post(() -> {
                    setSubmitting(false);
                    String role = latestProfile == null ? "" : latestProfile.getRole();
                    String status = latestDriverRequest == null
                            ? ""
                            : firstNonEmpty(
                                    latestDriverRequest.getLatestRequestStatus(),
                                    latestDriverRequest.getDriverStatus()
                            );
                    updateFormState(role, status);
                    renderError(exception, getString(R.string.submit_request_failed));
                });
            }
        });
    }

    private String uploadDocumentIfNeeded(Uri imageUri, String existingUrl, String jwtToken) throws IOException {
        if (imageUri == null) {
            return existingUrl;
        }

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> uploadedUrl = new AtomicReference<>("");
        AtomicReference<String> uploadError = new AtomicReference<>("");

        uploadService.uploadDocumentImage(imageUri, jwtToken, new UploadService.UploadCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                uploadedUrl.set(imageUrl);
                latch.countDown();
            }

            @Override
            public void onFailure(String errorMessage) {
                uploadError.set(errorMessage);
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Tải ảnh lên bị gián đoạn", exception);
        }

        if (!TextUtils.isEmpty(uploadError.get())) {
            throw new IOException(uploadError.get());
        }

        if (TextUtils.isEmpty(uploadedUrl.get())) {
            throw new IOException("Máy chủ không trả về đường dẫn ảnh");
        }

        return uploadedUrl.get();
    }

    private void updateDocumentPreview(ImageView imageView, Uri imageUri, String imageUrl) {
        if (imageUri != null) {
            imageView.setTag(imageUri.toString());
            imageView.setImageURI(imageUri);
            imageView.setImageTintList(null);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            return;
        }

        RemoteImageLoader.loadImage(imageView, imageUrl, R.drawable.ic_card);
    }

    private boolean hasDocument(Uri imageUri, String imageUrl) {
        return imageUri != null || !TextUtils.isEmpty(imageUrl);
    }

    private void updateFormState(String role, String status) {
        boolean isApproved = "Driver".equalsIgnoreCase(role) || "Approved".equalsIgnoreCase(status);
        boolean isPending = "Pending".equalsIgnoreCase(status);
        boolean canEdit = !isApproved && !isPending;

        btnPickIdCardImage.setEnabled(canEdit);
        btnPickDriverLicenseImage.setEnabled(canEdit);
        btnPickVehicleRegistrationImage.setEnabled(canEdit);
        btnVehicleMotor.setEnabled(canEdit);
        btnVehicleCar.setEnabled(canEdit);
        btnPickIdCardImage.setAlpha(canEdit ? 1f : 0.55f);
        btnPickDriverLicenseImage.setAlpha(canEdit ? 1f : 0.55f);
        btnPickVehicleRegistrationImage.setAlpha(canEdit ? 1f : 0.55f);
        btnVehicleMotor.setAlpha(canEdit ? 1f : 0.55f);
        btnVehicleCar.setAlpha(canEdit ? 1f : 0.55f);
        btnSubmitRequest.setEnabled(canEdit);

        if (isApproved) {
            btnSubmitRequest.setText(R.string.driver_request_approved_label);
        } else if (isPending) {
            btnSubmitRequest.setText(getString(R.string.profile_pending));
        } else if ("Rejected".equalsIgnoreCase(status)) {
            btnSubmitRequest.setText(getString(R.string.resubmit_request));
        } else {
            btnSubmitRequest.setText(getString(R.string.submit_request));
        }
    }

    private void setSubmitting(boolean isSubmitting) {
        if (isSubmitting) {
            btnSubmitRequest.setEnabled(false);
            btnSubmitRequest.setText(R.string.driver_request_submitting);
            return;
        }

        String role = latestProfile == null ? "" : latestProfile.getRole();
        String status = latestDriverRequest == null
                ? ""
                : firstNonEmpty(latestDriverRequest.getLatestRequestStatus(), latestDriverRequest.getDriverStatus());
        updateFormState(role, status);
    }

    private void selectVehicleType(String vehicleType) {
        selectedVehicleType = normalizeVehicleType(vehicleType);
        renderVehicleTypeSelection();
    }

    private void renderVehicleTypeSelection() {
        boolean isMotor = "motorbike".equals(selectedVehicleType);

        btnVehicleMotor.setBackgroundResource(isMotor ? R.drawable.bg_selected : R.drawable.bg_unselected);
        btnVehicleMotor.setTextColor(getColor(isMotor ? R.color.white : R.color.text_secondary));

        btnVehicleCar.setBackgroundResource(isMotor ? R.drawable.bg_unselected : R.drawable.bg_selected);
        btnVehicleCar.setTextColor(getColor(isMotor ? R.color.text_secondary : R.color.white));
    }

    private String normalizeVehicleType(String vehicleType) {
        if ("Taxi".equalsIgnoreCase(vehicleType) || "car".equalsIgnoreCase(vehicleType)) {
            return "car";
        }

        return "motorbike";
    }

    private void renderError(Exception exception, String fallbackMessage) {
        String message = exception.getMessage();
        if (message == null || message.trim().isEmpty()) {
            message = fallbackMessage;
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private String mapDriverStatus(String role, String status) {
        if ("Driver".equalsIgnoreCase(role) || "Approved".equalsIgnoreCase(status)) {
            return getString(R.string.driver_request_activated);
        }

        if ("Pending".equalsIgnoreCase(status)) {
            return getString(R.string.profile_pending_admin);
        }

        if ("Rejected".equalsIgnoreCase(status)) {
            return getString(R.string.profile_rejected);
        }

        return getString(R.string.driver_request_not_submitted);
    }

    private String buildStatusNote(DriverRequestStatus requestStatus) {
        if (requestStatus == null || TextUtils.isEmpty(requestStatus.getSubmittedAt())) {
            return getString(R.string.driver_request_desc);
        }

        return getString(R.string.request_submitted_at) + formatDate(requestStatus.getSubmittedAt());
    }

    private String formatDate(String rawDate) {
        if (TextUtils.isEmpty(rawDate)) {
            return "";
        }

        int separatorIndex = rawDate.indexOf('T');
        if (separatorIndex > 0) {
            return rawDate.substring(0, separatorIndex);
        }

        return rawDate;
    }

    private String firstNonEmpty(String... values) {
        for (String value : values) {
            if (!TextUtils.isEmpty(value)) {
                return value;
            }
        }
        return "";
    }
}
