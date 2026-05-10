package vnuhcm.hcmus.movify.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
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
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vnuhcm.hcmus.movify.R;
import vnuhcm.hcmus.movify.activities.LoadingActivity;
import vnuhcm.hcmus.movify.activities.profile.PersonalInfoActivity;
import vnuhcm.hcmus.movify.models.AccountProfile;
import vnuhcm.hcmus.movify.services.AuthApiService;
import vnuhcm.hcmus.movify.utils.NotificationWorkScheduler;
import vnuhcm.hcmus.movify.utils.RemoteImageLoader;

public class ProfileFragment extends Fragment {

    private static final String ARG_ROLE_MODE = "role_mode";
    private static final int REQUEST_NOTIFICATION_PERMISSION = 601;
    private static final int REQUEST_LOCATION_PERMISSION = 602;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private TextView tvUserName;
    private TextView tvPhone;
    private TextView tvEmail;
    private ImageView ivAvatar;

    private View itemPersonalInfo;
    private View itemPaymentMethod;
    private View itemNotification;
    private View itemLocation;
    private View dividerAccountInfo;
    private View dividerLocation;
    private SwitchCompat notificationSwitch;
    private SwitchCompat locationSwitch;

    private Button btnLogout;

    private AuthApiService authApiService;
    private ExecutorService profileExecutor;
    private String roleMode = BottomNavFragment.ROLE_USER;
    private boolean bindingSwitchState = false;

    public ProfileFragment() {
    }

    public static ProfileFragment newInstance(String roleMode) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ROLE_MODE, roleMode);
        fragment.setArguments(args);
        return fragment;
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
        if (getArguments() != null) {
            roleMode = getArguments().getString(ARG_ROLE_MODE, BottomNavFragment.ROLE_USER);
        }

        initViews(view);
        applyRoleVisibility();
        setupAccountInfo();
        setupSettings();
        setupActions();
        renderProfileLoading();
        loadProfile();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfile();
        syncPermissionSwitches();
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
        dividerAccountInfo = view.findViewById(R.id.dividerAccountInfo);
        dividerLocation = view.findViewById(R.id.dividerLocation);

        btnLogout = view.findViewById(R.id.btnLogout);
    }

    private void applyRoleVisibility() {
        if (!isAdminMode()) {
            return;
        }

        itemPaymentMethod.setVisibility(View.GONE);
        itemLocation.setVisibility(View.GONE);
        dividerAccountInfo.setVisibility(View.GONE);
        dividerLocation.setVisibility(View.GONE);
    }

    private void setupAccountInfo() {
        setupArrowItem(
                itemPersonalInfo,
                R.drawable.ic_user,
                getString(R.string.personal_info_title),
                getString(R.string.loading_account_info)
        );

        if (!isAdminMode()) {
            setupArrowItem(
                    itemPaymentMethod,
                    R.drawable.ic_wallet,
                    getString(R.string.payment_method_label),
                    getString(R.string.bank_transfer_desc)
            );
        }

        itemPersonalInfo.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), PersonalInfoActivity.class);
            intent.putExtra(PersonalInfoActivity.EXTRA_ROLE_MODE, roleMode);
            startActivity(intent);
        });

        if (!isAdminMode()) {
            itemPaymentMethod.setOnClickListener(v ->
                    Toast.makeText(requireContext(), R.string.payment_feature_coming, Toast.LENGTH_SHORT).show()
            );
        }
    }

    private void setupSettings() {
        notificationSwitch = setupSwitchItem(
                itemNotification,
                R.drawable.ic_noti,
                getString(R.string.notifications),
                getString(R.string.notifications_desc),
                areNotificationsEnabled()
        );
        notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> handleNotificationToggle(isChecked));
        itemNotification.setOnClickListener(v -> notificationSwitch.performClick());

        if (!isAdminMode()) {
            locationSwitch = setupSwitchItem(
                    itemLocation,
                    R.drawable.ic_location,
                    getString(R.string.location_title),
                    getString(R.string.location_permission_label),
                    hasLocationPermission()
            );
            locationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> handleLocationToggle(isChecked));
            itemLocation.setOnClickListener(v -> locationSwitch.performClick());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (granted) {
                NotificationWorkScheduler.schedule(requireContext());
            }
            syncPermissionSwitches();
            showPermissionResultToast(
                    granted,
                    "Đã bật thông báo",
                    "Chưa cấp quyền thông báo"
            );
            return;
        }

        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            syncPermissionSwitches();
            showPermissionResultToast(
                    grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED,
                    "Đã bật quyền vị trí",
                    "Chưa cấp quyền vị trí"
            );
        }
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

    private void renderProfileLoading() {
        tvUserName.setText(R.string.loading_dots);
        tvPhone.setText(R.string.loading_dots);
        tvEmail.setText(R.string.loading_dots);
        RemoteImageLoader.loadAvatar(ivAvatar, null);

        setupArrowItem(
                itemPersonalInfo,
                R.drawable.ic_user,
                getString(R.string.personal_info_title),
                getString(R.string.loading_account_info)
        );
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
        appendSegment(builder, mapRoleForDisplay(profile.getRole()));
        appendSegment(builder, mapGenderForDisplay(profile.getGender()));
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

    private String mapRoleForDisplay(String role) {
        if (BottomNavFragment.ROLE_ADMIN.equals(role)) {
            return getString(R.string.role_admin);
        }

        if (BottomNavFragment.ROLE_DRIVER.equals(role)) {
            return getString(R.string.role_driver);
        }

        if ("User".equalsIgnoreCase(role)) {
            return getString(R.string.role_user);
        }

        return role;
    }

    private String mapGenderForDisplay(String gender) {
        if ("Male".equalsIgnoreCase(gender)) {
            return getString(R.string.gender_male);
        }

        if ("Female".equalsIgnoreCase(gender)) {
            return getString(R.string.gender_female);
        }

        if ("Other".equalsIgnoreCase(gender)) {
            return getString(R.string.gender_other);
        }

        return gender;
    }

    private boolean isAdminMode() {
        return BottomNavFragment.ROLE_ADMIN.equals(roleMode);
    }

    private void setupArrowItem(View itemView, int iconRes, String title, String subtitle) {
        ImageView ivIcon = itemView.findViewById(R.id.ivIcon);
        TextView tvTitle = itemView.findViewById(R.id.tvTitle);
        TextView tvSubtitle = itemView.findViewById(R.id.tvSubtitle);

        ivIcon.setImageResource(iconRes);
        tvTitle.setText(title);
        tvSubtitle.setText(subtitle);
    }

    private SwitchCompat setupSwitchItem(View itemView, int iconRes, String title, String subtitle, boolean checked) {
        ImageView ivIcon = itemView.findViewById(R.id.ivIcon);
        TextView tvTitle = itemView.findViewById(R.id.tvTitle);
        TextView tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
        SwitchCompat switchItem = itemView.findViewById(R.id.switchItem);

        ivIcon.setImageResource(iconRes);
        tvTitle.setText(title);
        tvSubtitle.setText(subtitle);
        switchItem.setChecked(checked);
        return switchItem;
    }

    private void handleNotificationToggle(boolean isChecked) {
        if (bindingSwitchState || !isAdded()) {
            return;
        }

        if (isChecked) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATION_PERMISSION
                );
                return;
            }

            if (!NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()) {
                openNotificationSettings();
                Toast.makeText(requireContext(), "Bật thông báo trong cài đặt hệ thống", Toast.LENGTH_SHORT).show();
                syncPermissionSwitches();
                return;
            }

            NotificationWorkScheduler.schedule(requireContext());
            Toast.makeText(requireContext(), "Đã bật thông báo", Toast.LENGTH_SHORT).show();
            return;
        }

        NotificationWorkScheduler.cancel(requireContext());
        openNotificationSettings();
        Toast.makeText(requireContext(), "Tắt thông báo trong cài đặt hệ thống", Toast.LENGTH_SHORT).show();
        syncPermissionSwitches();
    }

    private void handleLocationToggle(boolean isChecked) {
        if (bindingSwitchState || !isAdded()) {
            return;
        }

        if (isChecked) {
            if (!hasLocationPermission()) {
                requestPermissions(
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                        REQUEST_LOCATION_PERMISSION
                );
                return;
            }

            Toast.makeText(requireContext(), "Đã bật quyền vị trí", Toast.LENGTH_SHORT).show();
            return;
        }

        openAppSettings();
        Toast.makeText(requireContext(), "Tắt quyền vị trí trong cài đặt ứng dụng", Toast.LENGTH_SHORT).show();
        syncPermissionSwitches();
    }

    private void syncPermissionSwitches() {
        if (!isAdded()) {
            return;
        }

        bindingSwitchState = true;
        if (notificationSwitch != null) {
            notificationSwitch.setChecked(areNotificationsEnabled());
        }
        if (locationSwitch != null) {
            locationSwitch.setChecked(hasLocationPermission());
        }
        bindingSwitchState = false;
    }

    private boolean areNotificationsEnabled() {
        if (!isAdded()) {
            return false;
        }

        boolean notificationManagerEnabled = NotificationManagerCompat.from(requireContext()).areNotificationsEnabled();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return notificationManagerEnabled;
        }

        return notificationManagerEnabled
                && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasLocationPermission() {
        if (!isAdded()) {
            return false;
        }

        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void openNotificationSettings() {
        Intent intent = new Intent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().getPackageName());
        } else {
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", requireContext().getPackageName(), null));
        }
        startActivity(intent);
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", requireContext().getPackageName(), null));
        startActivity(intent);
    }

    private void showPermissionResultToast(boolean granted, String grantedMessage, String deniedMessage) {
        if (!isAdded()) {
            return;
        }

        Toast.makeText(requireContext(), granted ? grantedMessage : deniedMessage, Toast.LENGTH_SHORT).show();
    }
}
