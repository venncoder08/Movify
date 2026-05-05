package vnuhcm.hcmus.movify.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

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
import vnuhcm.hcmus.movify.services.AuthApiService;
import vnuhcm.hcmus.movify.utils.DriverSearchNotificationHelper;

public class HomeActivity extends AppCompatActivity implements BottomNavFragment.MainNavigator {

    private static final String STATE_CURRENT_TAB = "state_current_tab";
    private static final String STATE_ROLE_MODE = "state_role_mode";
    private static final String STATE_ROLE_RESOLVED = "state_role_resolved";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private AuthApiService authApiService;
    private ExecutorService roleExecutor;
    private String currentTab = BottomNavFragment.TAB_BOOKING;
    private String roleMode = BottomNavFragment.ROLE_USER;
    private boolean roleResolved = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        authApiService = new AuthApiService(this);
        roleExecutor = Executors.newSingleThreadExecutor();

        if (savedInstanceState != null) {
            currentTab = savedInstanceState.getString(STATE_CURRENT_TAB, BottomNavFragment.TAB_BOOKING);
            roleMode = savedInstanceState.getString(STATE_ROLE_MODE, BottomNavFragment.ROLE_USER);
            roleResolved = savedInstanceState.getBoolean(STATE_ROLE_RESOLVED, false);
        }

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
        mainHandler.removeCallbacksAndMessages(null);
        if (roleExecutor != null) {
            roleExecutor.shutdownNow();
            roleExecutor = null;
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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_CURRENT_TAB, currentTab);
        outState.putString(STATE_ROLE_MODE, roleMode);
        outState.putBoolean(STATE_ROLE_RESOLVED, roleResolved);
    }

    private void loadScreen(Fragment fragment, String tab) {
        findViewById(R.id.bottomNavContainer).setVisibility(View.VISIBLE);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.contentContainer, fragment)
                .commit();

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.bottomNavContainer, BottomNavFragment.newInstance(tab, roleMode))
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
                .replace(R.id.bottomNavContainer, BottomNavFragment.newInstance(BottomNavFragment.TAB_BOOKING, roleMode))
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
}
