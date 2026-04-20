package vnuhcm.hcmus.movify.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vnuhcm.hcmus.movify.R;
import vnuhcm.hcmus.movify.activities.LoadingActivity;
import vnuhcm.hcmus.movify.activities.profile.PersonalInfoActivity;
import vnuhcm.hcmus.movify.models.AccountProfile;
import vnuhcm.hcmus.movify.services.AuthApiService;
import vnuhcm.hcmus.movify.utils.RemoteImageLoader;

public class ProfileFragment extends Fragment {

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private TextView tvUserName;
    private TextView tvPhone;
    private TextView tvEmail;
    private ImageView ivAvatar;

    private View itemPersonalInfo;
    private View itemPaymentMethod;
    private View itemNotification;
    private View itemLocation;

    private Button btnLogout;

    private AuthApiService authApiService;
    private ExecutorService profileExecutor;

    public ProfileFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authApiService = new AuthApiService(requireContext());
        profileExecutor = Executors.newSingleThreadExecutor();

        initViews(view);
        setupAccountInfo();
        setupSettings();
        setupActions();
        loadProfile();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mainHandler.removeCallbacksAndMessages(null);
        if (profileExecutor != null) {
            profileExecutor.shutdownNow();
            profileExecutor = null;
        }
    }

    private void initViews(View view) {
        tvUserName = view.findViewById(R.id.tvUserName);
        tvPhone = view.findViewById(R.id.tvPhone);
        tvEmail = view.findViewById(R.id.tvEmail);
        ivAvatar = view.findViewById(R.id.ivAvatar);

        itemPersonalInfo = view.findViewById(R.id.itemPersonalInfo);
        itemPaymentMethod = view.findViewById(R.id.itemPaymentMethod);
        itemNotification = view.findViewById(R.id.itemNotification);
        itemLocation = view.findViewById(R.id.itemLocation);

        btnLogout = view.findViewById(R.id.btnLogout);
    }

    private void setupAccountInfo() {
        setupArrowItem(
                itemPersonalInfo,
                R.drawable.ic_user,
                getString(R.string.personal_info_title),
                getString(R.string.loading_account_info)
        );

        setupArrowItem(
                itemPaymentMethod,
                R.drawable.ic_wallet,
                getString(R.string.payment_method_label),
                getString(R.string.bank_transfer_desc)
        );

        itemPersonalInfo.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), PersonalInfoActivity.class);
            startActivity(intent);
        });

        itemPaymentMethod.setOnClickListener(v ->
                Toast.makeText(requireContext(), R.string.payment_feature_coming, Toast.LENGTH_SHORT).show()
        );
    }

    private void setupSettings() {
        setupSwitchItem(
                itemNotification,
                R.drawable.ic_noti,
                "Thông báo",
                "Quản lý thông báo ứng dụng",
                true
        );

        setupSwitchItem(
                itemLocation,
                R.drawable.ic_location,
                getString(R.string.location_title),
                getString(R.string.location_permission_label),
                true
        );
    }

    private void setupActions() {
        btnLogout.setOnClickListener(v -> performLogout());
    }

    private void loadProfile() {
        if (profileExecutor == null || profileExecutor.isShutdown()) {
            return;
        }

        profileExecutor.execute(() -> {
            try {
                AccountProfile profile = authApiService.fetchCurrentUserProfile();
                mainHandler.post(() -> bindProfile(profile));
            } catch (Exception exception) {
                mainHandler.post(() -> handleProfileError(exception));
            }
        });
    }

    private void bindProfile(@NonNull AccountProfile profile) {
        if (!isAdded()) {
            return;
        }

        String fullName = fallbackValue(profile.getFullName(), buildDisplayName(profile.getEmail()));
        String phoneNumber = fallbackValue(profile.getPhoneNumber(), "Chưa cập nhật số điện thoại");
        String email = fallbackValue(profile.getEmail(), authApiService.getUserEmail());

        tvUserName.setText(fullName);
        tvPhone.setText(phoneNumber);
        tvEmail.setText(fallbackValue(email, "Chưa cập nhật email"));
        RemoteImageLoader.loadAvatar(ivAvatar, profile.getAvatar());

        setupArrowItem(
                itemPersonalInfo,
                R.drawable.ic_user,
                "Thông tin cá nhân",
                buildPersonalInfoSubtitle(profile)
        );
    }

    private void handleProfileError(Exception exception) {
        if (!isAdded()) {
            return;
        }

        String cachedEmail = authApiService.getUserEmail();
        tvUserName.setText(buildDisplayName(cachedEmail));
        tvPhone.setText("Chưa cập nhật số điện thoại");
        tvEmail.setText(fallbackValue(cachedEmail, "Chưa cập nhật email"));
        RemoteImageLoader.loadAvatar(ivAvatar, null);

        setupArrowItem(
                itemPersonalInfo,
                R.drawable.ic_user,
                "Thông tin cá nhân",
                "Không tải được profile"
        );

        String errorMessage = exception.getMessage();
        if (TextUtils.isEmpty(errorMessage)) {
            errorMessage = "Không tải được thông tin tài khoản";
        }

        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
    }

    private void performLogout() {
        btnLogout.setEnabled(false);
        btnLogout.setText("Đăng xuất...");

        new Thread(() -> {
            try {
                if (authApiService.isLoggedIn()) {
                    authApiService.logout();
                }
            } catch (Exception ignored) {
                // Ignore backend logout failures and always clear local session.
            } finally {
                authApiService.clearSession();
                mainHandler.post(this::navigateToLoading);
            }
        }).start();
    }

    private void navigateToLoading() {
        if (!isAdded()) {
            return;
        }

        Intent intent = new Intent(requireActivity(), LoadingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    private String buildPersonalInfoSubtitle(AccountProfile profile) {
        StringBuilder builder = new StringBuilder();
        appendSegment(builder, profile.getRole());
        appendSegment(builder, profile.getGender());
        appendSegment(builder, formatDate(profile.getDateOfBirth()));

        if (builder.length() == 0) {
            return "Thông tin tài khoản đã được đồng bộ";
        }

        return builder.toString();
    }

    private void appendSegment(StringBuilder builder, String value) {
        if (TextUtils.isEmpty(value)) {
            return;
        }

        if (builder.length() > 0) {
            builder.append(" | ");
        }
        builder.append(value);
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

    private String buildDisplayName(String email) {
        if (TextUtils.isEmpty(email)) {
            return "Tài khoản Movify";
        }

        int separatorIndex = email.indexOf('@');
        if (separatorIndex > 0) {
            return email.substring(0, separatorIndex);
        }

        return email;
    }

    private String fallbackValue(String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value;
    }

    private void setupArrowItem(View itemView, int iconRes, String title, String subtitle) {
        ImageView ivIcon = itemView.findViewById(R.id.ivIcon);
        TextView tvTitle = itemView.findViewById(R.id.tvTitle);
        TextView tvSubtitle = itemView.findViewById(R.id.tvSubtitle);

        ivIcon.setImageResource(iconRes);
        tvTitle.setText(title);
        tvSubtitle.setText(subtitle);
    }

    private void setupSwitchItem(View itemView, int iconRes, String title, String subtitle, boolean checked) {
        ImageView ivIcon = itemView.findViewById(R.id.ivIcon);
        TextView tvTitle = itemView.findViewById(R.id.tvTitle);
        TextView tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
        SwitchCompat switchItem = itemView.findViewById(R.id.switchItem);

        ivIcon.setImageResource(iconRes);
        tvTitle.setText(title);
        tvSubtitle.setText(subtitle);
        switchItem.setChecked(checked);

        switchItem.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String message = title + ": " + (isChecked ? "Bật" : "Tắt");
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        });
    }
}
