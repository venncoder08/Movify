package vnuhcm.hcmus.movify.activities.profile;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vnuhcm.hcmus.movify.R;
import vnuhcm.hcmus.movify.models.AccountProfile;
import vnuhcm.hcmus.movify.models.DriverRequestStatus;
import vnuhcm.hcmus.movify.services.AuthApiService;
import vnuhcm.hcmus.movify.services.DriverRequestService;

public class BecomeDriverActivity extends AppCompatActivity {

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private ExecutorService executorService;
    private AuthApiService authApiService;

    private TextView tvStatusValue;
    private TextView tvStatusNote;
    private EditText etIdCard;
    private EditText etDriverLicense;
    private EditText etVehicleRegistration;
    private TextView btnSubmitRequest;

    private AccountProfile latestProfile;
    private DriverRequestStatus latestDriverRequest;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_become_driver);

        authApiService = new AuthApiService(this);
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
        etIdCard = findViewById(R.id.etIdCard);
        etDriverLicense = findViewById(R.id.etDriverLicense);
        etVehicleRegistration = findViewById(R.id.etVehicleRegistration);
        btnSubmitRequest = findViewById(R.id.btnSubmitRequest);

        ivBack.setOnClickListener(v -> finish());
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
                mainHandler.post(() -> renderError(exception, "Không tải được hồ sơ tài xế"));
            }
        });
    }

    private void bindState(AccountProfile profile, DriverRequestStatus requestStatus) {
        latestProfile = profile;
        latestDriverRequest = requestStatus;

        fillIfChanged(etIdCard, firstNonEmpty(requestStatus.getIdCard(), profile.getIdCard()));
        fillIfChanged(etDriverLicense, firstNonEmpty(requestStatus.getDriverLicense(), profile.getDriverLicense()));
        fillIfChanged(etVehicleRegistration, firstNonEmpty(requestStatus.getVehicleRegistration(), profile.getVehicleRegistration()));

        String effectiveStatus = firstNonEmpty(
                requestStatus.getLatestRequestStatus(),
                requestStatus.getDriverStatus(),
                profile.getDriverStatus()
        );
        tvStatusValue.setText(mapDriverStatus(profile.getRole(), effectiveStatus));
        tvStatusNote.setText(buildStatusNote(requestStatus));
        updateFormState(profile.getRole(), effectiveStatus);
    }

    private void submitDriverRequest() {
        String idCard = etIdCard.getText().toString().trim();
        String driverLicense = etDriverLicense.getText().toString().trim();
        String vehicleRegistration = etVehicleRegistration.getText().toString().trim();

        if (TextUtils.isEmpty(idCard) || TextUtils.isEmpty(driverLicense) || TextUtils.isEmpty(vehicleRegistration)) {
            Toast.makeText(this, R.string.fill_required_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        if (executorService == null || executorService.isShutdown()) {
            return;
        }

        setSubmitting(true);
        executorService.execute(() -> {
            try {
                String message = DriverRequestService.submitDriverRequest(this, idCard, driverLicense, vehicleRegistration);
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

    private void updateFormState(String role, String status) {
        boolean isApproved = "Driver".equalsIgnoreCase(role) || "Approved".equalsIgnoreCase(status);
        boolean isPending = "Pending".equalsIgnoreCase(status);
        boolean canEdit = !isApproved && !isPending;

        etIdCard.setEnabled(canEdit);
        etDriverLicense.setEnabled(canEdit);
        etVehicleRegistration.setEnabled(canEdit);
        btnSubmitRequest.setEnabled(canEdit);

        if (isApproved) {
            btnSubmitRequest.setText("Tài khoản đã là tài xế");
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
            btnSubmitRequest.setText("Đang gửi...");
            return;
        }

        String role = latestProfile == null ? "" : latestProfile.getRole();
        String status = latestDriverRequest == null
                ? ""
                : firstNonEmpty(latestDriverRequest.getLatestRequestStatus(), latestDriverRequest.getDriverStatus());
        updateFormState(role, status);
    }

    private void renderError(Exception exception, String fallbackMessage) {
        String message = exception.getMessage();
        if (message == null || message.trim().isEmpty()) {
            message = fallbackMessage;
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void fillIfChanged(EditText editText, String value) {
        if (TextUtils.isEmpty(editText.getText().toString().trim()) && !TextUtils.isEmpty(value)) {
            editText.setText(value);
        }
    }

    private String mapDriverStatus(String role, String status) {
        if ("Driver".equalsIgnoreCase(role) || "Approved".equalsIgnoreCase(status)) {
            return "Đã được kích hoạt tài xế";
        }

        if ("Pending".equalsIgnoreCase(status)) {
            return getString(R.string.profile_pending_admin);
        }

        if ("Rejected".equalsIgnoreCase(status)) {
            return getString(R.string.profile_rejected);
        }

        return "Chưa gửi hồ sơ";
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
