package vnuhcm.hcmus.movify.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vnuhcm.hcmus.movify.R;
import vnuhcm.hcmus.movify.fragments.AdminPromotionFragment;
import vnuhcm.hcmus.movify.fragments.AdminRequestFragment;
import vnuhcm.hcmus.movify.fragments.BookingFragment;
import vnuhcm.hcmus.movify.fragments.BottomNavFragment;
import vnuhcm.hcmus.movify.fragments.DriverMapFragment;
import vnuhcm.hcmus.movify.fragments.HistoryFragment;
import vnuhcm.hcmus.movify.fragments.HomeLoadingFragment;
import vnuhcm.hcmus.movify.fragments.MapBookingFragment;
import vnuhcm.hcmus.movify.fragments.MessageFragment;
import vnuhcm.hcmus.movify.fragments.NotificationFragment;
import vnuhcm.hcmus.movify.fragments.ProfileFragment;
import vnuhcm.hcmus.movify.models.AccountProfile;
import vnuhcm.hcmus.movify.models.NotificationFeed;
import vnuhcm.hcmus.movify.services.AuthApiService;
import vnuhcm.hcmus.movify.services.MessageService;
import vnuhcm.hcmus.movify.services.NotificationService;
import vnuhcm.hcmus.movify.services.SocketManager;
import vnuhcm.hcmus.movify.utils.DriverSearchNotificationHelper;
import vnuhcm.hcmus.movify.utils.NotificationWorkScheduler;
import vnuhcm.hcmus.movify.utils.SystemNotificationHelper;

public class HomeActivity extends AppCompatActivity
        implements BottomNavFragment.MainNavigator, SocketManager.SocketEventListener {

    public static final String EXTRA_OPEN_TAB = "vnuhcm.hcmus.movify.extra.OPEN_TAB";

    private static final String STATE_CURRENT_TAB = "state_current_tab";
    private static final String STATE_ROLE_MODE = "state_role_mode";
    private static final String STATE_ROLE_RESOLVED = "state_role_resolved";
    private static final String STATE_HAS_UNREAD_NOTIFICATIONS = "state_has_unread_notifications";
    private static final String STATE_HAS_UNREAD_MESSAGES = "state_has_unread_messages";
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 501;
    private static final long SYSTEM_NOTIFICATION_POLL_INTERVAL_MS = 10_000L;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private AuthApiService authApiService;
    private ExecutorService roleExecutor;
    private ExecutorService notificationExecutor;
    private Runnable notificationPollRunnable;
    private String currentTab = BottomNavFragment.TAB_BOOKING;
    private String roleMode = BottomNavFragment.ROLE_USER;
    private boolean roleResolved = false;
    private volatile boolean hasUnreadNotifications = false;
    private volatile boolean hasUnreadMessages = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        authApiService = new AuthApiService(this);
        roleExecutor = Executors.newSingleThreadExecutor();
        notificationExecutor = Executors.newSingleThreadExecutor();
        SocketManager.getInstance(this).connect();
        SocketManager.getInstance(this).addListener(this);

        if (savedInstanceState != null) {
            currentTab = savedInstanceState.getString(STATE_CURRENT_TAB, BottomNavFragment.TAB_BOOKING);
            roleMode = savedInstanceState.getString(STATE_ROLE_MODE, BottomNavFragment.ROLE_USER);
            roleResolved = savedInstanceState.getBoolean(STATE_ROLE_RESOLVED, false);
            hasUnreadNotifications = savedInstanceState.getBoolean(STATE_HAS_UNREAD_NOTIFICATIONS, false);
            hasUnreadMessages = savedInstanceState.getBoolean(STATE_HAS_UNREAD_MESSAGES, false);
        }

        handleOpenTabIntent(getIntent());
        ensureNotificationPermission();
        NotificationWorkScheduler.schedule(this);
        startSystemNotificationPolling();

        if (roleResolved) {
            refreshCurrentTab();
        } else {
            showRoleLoading();
        }

        resolveRoleMode();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SocketManager.getInstance(this).removeListener(this);
        mainHandler.removeCallbacksAndMessages(null);
        if (roleExecutor != null) {
            roleExecutor.shutdownNow();
            roleExecutor = null;
        }
        if (notificationExecutor != null) {
            notificationExecutor.shutdownNow();
            notificationExecutor = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (roleResolved) {
            refreshUnreadBadges();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleOpenTabIntent(intent);
        if (roleResolved) {
            refreshCurrentTab();
        }
    }

    /**
     * When the user presses Back while a driver search is active, show a
     * confirmation dialog instead of silently quitting.
     */
    @Override
    public void onBackPressed() {
        if (DriverSearchNotificationHelper.isSearching()) {
            showExitWhileSearchingDialog();
            return;
        }
        super.onBackPressed();
    }

    /**
     * Fired when the user presses Home or switches apps.
     * Posts the "still searching" notification so it appears in the shade.
     */
    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        NotificationWorkScheduler.startBackgroundWatch(getApplicationContext());
        if (DriverSearchNotificationHelper.isSearching()) {
            DriverSearchNotificationHelper.notifyAppExited(getApplicationContext());
        }
    }

    private void showExitWhileSearchingDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.exit_search_dialog_title))
                .setMessage(getString(R.string.exit_search_dialog_message))
                .setPositiveButton(getString(R.string.exit_search_dialog_confirm), (dialog, which) -> {
                    DriverSearchNotificationHelper.notifyAppExited(getApplicationContext());
                    // Small delay so notification is posted before activity finishes
                    mainHandler.postDelayed(this::finishAffinity, 300);
                })
                .setNegativeButton(getString(R.string.exit_search_dialog_cancel), null)
                .show();
    }

    private void ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }

        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                NOTIFICATION_PERMISSION_REQUEST_CODE
        );
    }

    private void startSystemNotificationPolling() {
        pollSystemNotifications();
    }

    private void pollSystemNotifications() {
        if (notificationExecutor == null || notificationExecutor.isShutdown()) {
            return;
        }

        Context appContext = getApplicationContext();
        notificationExecutor.execute(() -> {
            UnreadBadgeState badgeState = loadUnreadBadgeState(appContext, true);
            mainHandler.post(() -> {
                applyUnreadBadges(badgeState);
                scheduleNextNotificationPoll();
            });
        });
    }

    private void scheduleNextNotificationPoll() {
        if (isFinishing() || isDestroyed()
                || notificationExecutor == null
                || notificationExecutor.isShutdown()) {
            return;
        }

        if (notificationPollRunnable != null) {
            mainHandler.removeCallbacks(notificationPollRunnable);
        }
        notificationPollRunnable = this::pollSystemNotifications;
        mainHandler.postDelayed(notificationPollRunnable, SYSTEM_NOTIFICATION_POLL_INTERVAL_MS);
    }

    private void handleOpenTabIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        String requestedTab = intent.getStringExtra(EXTRA_OPEN_TAB);
        if (BottomNavFragment.TAB_NOTIFICATION.equals(requestedTab)) {
            currentTab = BottomNavFragment.TAB_NOTIFICATION;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_CURRENT_TAB, currentTab);
        outState.putString(STATE_ROLE_MODE, roleMode);
        outState.putBoolean(STATE_ROLE_RESOLVED, roleResolved);
        outState.putBoolean(STATE_HAS_UNREAD_NOTIFICATIONS, hasUnreadNotifications);
        outState.putBoolean(STATE_HAS_UNREAD_MESSAGES, hasUnreadMessages);
    }

    private void loadScreen(Fragment fragment, String tab) {
        findViewById(R.id.bottomNavContainer).setVisibility(View.VISIBLE);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.contentContainer, fragment)
                .commit();

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.bottomNavContainer, BottomNavFragment.newInstance(
                        tab,
                        roleMode,
                        hasUnreadNotifications,
                        hasUnreadMessages
                ))
                .commit();
    }

    private void showRoleLoading() {
        findViewById(R.id.bottomNavContainer).setVisibility(View.GONE);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.contentContainer, new HomeLoadingFragment())
                .commit();
    }

    @Override
    public void openBooking() {
        currentTab = BottomNavFragment.TAB_BOOKING;
        refreshCurrentTab();
    }

    @Override
    public void openProfile() {
        currentTab = BottomNavFragment.TAB_PROFILE;
        refreshCurrentTab();
    }

    @Override
    public void openNotification() {
        currentTab = BottomNavFragment.TAB_NOTIFICATION;
        refreshCurrentTab();
    }

    @Override
    public void openHistory() {
        currentTab = BottomNavFragment.TAB_HISTORY;
        refreshCurrentTab();
    }

    @Override
    public void openMessage() {
        if (isAdminMode()) {
            currentTab = BottomNavFragment.TAB_BOOKING;
            refreshCurrentTab();
            return;
        }

        currentTab = BottomNavFragment.TAB_MESSAGE;
        refreshCurrentTab();
    }

    public void openMapBooking() {
        if (isAdminMode()) {
            return;
        }

        Fragment mapFragment;
        if (isDriverMode()) {
            mapFragment = new DriverMapFragment();
        } else {
            mapFragment = new MapBookingFragment();
        }

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.contentContainer, mapFragment)
                .addToBackStack(null)
                .commit();

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.bottomNavContainer, BottomNavFragment.newInstance(
                        BottomNavFragment.TAB_BOOKING,
                        roleMode,
                        hasUnreadNotifications,
                        hasUnreadMessages
                ))
                .commit();
    }

    private void refreshCurrentTab() {
        loadScreen(createFragmentForCurrentTab(), currentTab);
    }

    private Fragment createFragmentForCurrentTab() {
        if (isAdminMode()) {
            return createAdminFragmentForCurrentTab();
        }

        switch (currentTab) {
            case BottomNavFragment.TAB_PROFILE:
                return ProfileFragment.newInstance(roleMode);
            case BottomNavFragment.TAB_NOTIFICATION:
                return new NotificationFragment();
            case BottomNavFragment.TAB_HISTORY:
                return HistoryFragment.newInstance(isDriverMode());
            case BottomNavFragment.TAB_MESSAGE:
                return new MessageFragment();
            case BottomNavFragment.TAB_BOOKING:
            default:
                return BookingFragment.newInstance(isDriverMode());
        }
    }

    private Fragment createAdminFragmentForCurrentTab() {
        switch (currentTab) {
            case BottomNavFragment.TAB_PROFILE:
                return ProfileFragment.newInstance(roleMode);
            case BottomNavFragment.TAB_NOTIFICATION:
                return new NotificationFragment();
            case BottomNavFragment.TAB_HISTORY:
                return new AdminPromotionFragment();
            case BottomNavFragment.TAB_BOOKING:
            default:
                return new AdminRequestFragment();
        }
    }

    private void resolveRoleMode() {
        if (roleExecutor == null || roleExecutor.isShutdown()) {
            return;
        }

        roleExecutor.execute(() -> {
            try {
                AccountProfile profile = authApiService.fetchCurrentUserProfile();
                String resolvedRoleMode = resolveRoleMode(profile);
                mainHandler.post(() -> applyRoleMode(resolvedRoleMode));
            } catch (Exception ignored) {
                mainHandler.post(this::handleRoleLoadFailure);
            }
        });
    }

    private void applyRoleMode(String resolvedRoleMode) {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        boolean initialRender = !roleResolved;
        boolean roleChanged = !roleMode.equals(resolvedRoleMode);
        roleMode = resolvedRoleMode;
        roleResolved = true;
        if (isAdminMode() && BottomNavFragment.TAB_MESSAGE.equals(currentTab)) {
            currentTab = BottomNavFragment.TAB_BOOKING;
        }

        if (initialRender || roleChanged) {
            refreshCurrentTab();
        }
        refreshUnreadBadges();
    }

    private void handleRoleLoadFailure() {
        if (isFinishing() || isDestroyed() || roleResolved) {
            return;
        }

        roleResolved = true;
        refreshCurrentTab();
    }

    private String resolveRoleMode(AccountProfile profile) {
        if (profile == null) {
            return BottomNavFragment.ROLE_USER;
        }

        String role = profile.getRole();
        if (role != null && role.toLowerCase().contains("admin")) {
            return BottomNavFragment.ROLE_ADMIN;
        }

        if ("Driver".equalsIgnoreCase(role) || "Approved".equalsIgnoreCase(profile.getDriverStatus())) {
            return BottomNavFragment.ROLE_DRIVER;
        }

        return BottomNavFragment.ROLE_USER;
    }

    private boolean isDriverMode() {
        return BottomNavFragment.ROLE_DRIVER.equals(roleMode);
    }

    private boolean isAdminMode() {
        return BottomNavFragment.ROLE_ADMIN.equals(roleMode);
    }

    public void setNotificationUnreadBadge(boolean hasUnread) {
        hasUnreadNotifications = hasUnread;
        updateBottomNavBadges();
    }

    public void setMessageUnreadBadge(boolean hasUnread) {
        hasUnreadMessages = !isAdminMode() && hasUnread;
        updateBottomNavBadges();
    }

    public void refreshUnreadBadges() {
        if (notificationExecutor == null || notificationExecutor.isShutdown()) {
            return;
        }

        Context appContext = getApplicationContext();
        notificationExecutor.execute(() -> {
            UnreadBadgeState badgeState = loadUnreadBadgeState(appContext, false);
            mainHandler.post(() -> applyUnreadBadges(badgeState));
        });
    }

    private UnreadBadgeState loadUnreadBadgeState(Context appContext, boolean postSystemNotifications) {
        boolean notificationUnread = hasUnreadNotifications;
        boolean messageUnread = isAdminMode() ? false : hasUnreadMessages;

        try {
            NotificationFeed feed = NotificationService.fetchNotifications(appContext);
            if (postSystemNotifications) {
                SystemNotificationHelper.notifyNewUnread(appContext, feed.getNotifications());
            }
            notificationUnread = feed.getUnreadCount() > 0;
        } catch (Exception ignored) {
            // Badge polling should not interrupt the active screen.
        }

        if (isAdminMode()) {
            messageUnread = false;
        } else {
            try {
                messageUnread = MessageService.fetchUnreadCount(appContext) > 0;
            } catch (Exception ignored) {
                // Keep the previous state if the message count cannot be refreshed.
            }
        }

        return new UnreadBadgeState(notificationUnread, messageUnread);
    }

    private void applyUnreadBadges(UnreadBadgeState badgeState) {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        hasUnreadNotifications = badgeState.hasUnreadNotifications;
        hasUnreadMessages = !isAdminMode() && badgeState.hasUnreadMessages;
        updateBottomNavBadges();
    }

    private void updateBottomNavBadges() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.bottomNavContainer);
        if (fragment instanceof BottomNavFragment) {
            ((BottomNavFragment) fragment).setUnreadBadges(hasUnreadNotifications, hasUnreadMessages);
        }
    }

    @Override
    public void onSocketEvent(@NonNull String eventName, @Nullable JSONObject payload) {
        if ("message:new".equals(eventName)) {
            JSONObject message = payload == null ? null : payload.optJSONObject("message");
            if (message != null && !message.optBoolean("isMine", false)) {
                setMessageUnreadBadge(true);
            }
            return;
        }

        if ("conversation:read".equals(eventName)) {
            refreshUnreadBadges();
        }
    }

    private static final class UnreadBadgeState {
        private final boolean hasUnreadNotifications;
        private final boolean hasUnreadMessages;

        private UnreadBadgeState(boolean hasUnreadNotifications, boolean hasUnreadMessages) {
            this.hasUnreadNotifications = hasUnreadNotifications;
            this.hasUnreadMessages = hasUnreadMessages;
        }
    }
}
