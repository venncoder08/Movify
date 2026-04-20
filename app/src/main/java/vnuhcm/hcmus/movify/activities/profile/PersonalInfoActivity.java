package vnuhcm.hcmus.movify.activities.profile;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vnuhcm.hcmus.movify.R;
import vnuhcm.hcmus.movify.models.AccountProfile;
import vnuhcm.hcmus.movify.services.AuthApiService;
import vnuhcm.hcmus.movify.utils.RemoteImageLoader;

public class PersonalInfoActivity extends AppCompatActivity {

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private AuthApiService authApiService;
    private ExecutorService executorService;

    private ImageView ivAvatar;
    private TextView tvFullNameValue;
    private TextView tvEmailValue;
    private TextView tvPhoneValue;
    private TextView tvRoleValue;
    private TextView tvDriverStatusValue;
    private TextView tvGenderValue;
    private TextView tvDateOfBirthValue;
    private TextView btnEditProfile;
    private TextView btnBecomeDriver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_info);

        authApiService = new AuthApiService(this);
        executorService = Executors.newSingleThreadExecutor();

        initViews();
        loadProfile();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfile();
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
        ivAvatar = findViewById(R.id.ivAvatar);
        tvFullNameValue = findViewById(R.id.tvFullNameValue);
        tvEmailValue = findViewById(R.id.tvEmailValue);
        tvPhoneValue = findViewById(R.id.tvPhoneValue);
        tvRoleValue = findViewById(R.id.tvRoleValue);
        tvDriverStatusValue = findViewById(R.id.tvDriverStatusValue);
        tvGenderValue = findViewById(R.id.tvGenderValue);
        tvDateOfBirthValue = findViewById(R.id.tvDateOfBirthValue);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnBecomeDriver = findViewById(R.id.btnBecomeDriver);

        ivBack.setOnClickListener(v -> finish());
        btnEditProfile.setOnClickListener(v ->
                Toast.makeText(this, R.string.profile_edit_feature_coming, Toast.LENGTH_SHORT).show()
        );
        btnBecomeDriver.setOnClickListener(v ->
                startActivity(new Intent(this, BecomeDriverActivity.class))
        );
    }

    private void loadProfile() {
        if (executorService == null || executorService.isShutdown()) {
            return;
        }

        executorService.execute(() -> {
            try {
                AccountProfile profile = authApiService.fetchCurrentUserProfile();
                mainHandler.post(() -> bindProfile(profile));
            } catch (Exception exception) {
                mainHandler.post(() -> renderFallback(exception));
            }
        });
    }

    private void bindProfile(AccountProfile profile) {
        RemoteImageLoader.loadAvatar(ivAvatar, profile.getAvatar());
        tvFullNameValue.setText(fallback(profile.getFullName(), buildDisplayName(profile.getEmail())));
        tvEmailValue.setText(fallback(profile.getEmail(), authApiService.getUserEmail()));
        tvPhoneValue.setText(fallback(profile.getPhoneNumber(), getString(R.string.not_updated)));
        tvRoleValue.setText(fallback(profile.getRole(), getString(R.string.not_updated)));
        tvDriverStatusValue.setText(resolveDriverStatus(profile));
        tvGenderValue.setText(fallback(profile.getGender(), getString(R.string.not_updated)));
        tvDateOfBirthValue.setText(fallback(formatDate(profile.getDateOfBirth()), getString(R.string.not_updated)));
        updateBecomeDriverButton(profile);
    }

    private void renderFallback(Exception exception) {
        String cachedEmail = authApiService.getUserEmail();
        RemoteImageLoader.loadAvatar(ivAvatar, null);
        tvFullNameValue.setText(buildDisplayName(cachedEmail));
        tvEmailValue.setText(fallback(cachedEmail, getString(R.string.not_updated)));
        tvPhoneValue.setText(getString(R.string.not_updated));
        tvRoleValue.setText(getString(R.string.not_updated));
        tvDriverStatusValue.setText(getString(R.string.not_updated));
        tvGenderValue.setText(getString(R.string.not_updated));
        tvDateOfBirthValue.setText(getString(R.string.not_updated));
        btnBecomeDriver.setEnabled(true);
        btnBecomeDriver.setText(R.string.become_driver);

        String errorMessage = exception.getMessage();
        if (TextUtils.isEmpty(errorMessage)) {
            errorMessage = getString(R.string.profile_load_error);
        }
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
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

    private String fallback(String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value;
    }

    private String resolveDriverStatus(AccountProfile profile) {
        if ("Driver".equalsIgnoreCase(profile.getRole()) || "Approved".equalsIgnoreCase(profile.getDriverStatus())) {
            return getString(R.string.already_driver);
        }

        if ("Pending".equalsIgnoreCase(profile.getDriverStatus())) {
            return getString(R.string.pending_approval);
        }

        if ("Rejected".equalsIgnoreCase(profile.getDriverStatus())) {
            return getString(R.string.rejected_status);
        }

        return getString(R.string.not_registered);
    }

    private void updateBecomeDriverButton(AccountProfile profile) {
        if ("Driver".equalsIgnoreCase(profile.getRole()) || "Approved".equalsIgnoreCase(profile.getDriverStatus())) {
            btnBecomeDriver.setEnabled(false);
            btnBecomeDriver.setText(R.string.you_are_driver);
            return;
        }

        if ("Pending".equalsIgnoreCase(profile.getDriverStatus())) {
            btnBecomeDriver.setEnabled(true);
            btnBecomeDriver.setText(R.string.view_driver_profile);
            return;
        }

        if ("Rejected".equalsIgnoreCase(profile.getDriverStatus())) {
            btnBecomeDriver.setEnabled(true);
            btnBecomeDriver.setText(R.string.resubmit_driver_profile);
            return;
        }

        btnBecomeDriver.setEnabled(true);
        btnBecomeDriver.setText(R.string.become_driver);
    }
}
