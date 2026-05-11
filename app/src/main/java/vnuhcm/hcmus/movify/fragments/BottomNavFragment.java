package vnuhcm.hcmus.movify.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import vnuhcm.hcmus.movify.R;

public class BottomNavFragment extends Fragment {

    private static final String ARG_CURRENT_TAB = "current_tab";
    private static final String ARG_ROLE_MODE = "role_mode";
    private static final String ARG_HAS_UNREAD_NOTIFICATIONS = "has_unread_notifications";
    private static final String ARG_HAS_UNREAD_MESSAGES = "has_unread_messages";

    public static final String ROLE_USER = "user";
    public static final String ROLE_DRIVER = "driver";
    public static final String ROLE_ADMIN = "admin";
    public static final String TAB_PROFILE = "profile";
    public static final String TAB_NOTIFICATION = "notification";
    public static final String TAB_BOOKING = "booking";
    public static final String TAB_HISTORY = "history";
    public static final String TAB_MESSAGE = "message";

    private String currentTab = TAB_BOOKING;
    private String roleMode = ROLE_USER;
    private boolean hasUnreadNotifications = false;
    private boolean hasUnreadMessages = false;

    private LinearLayout tabProfile, tabNotification, tabBooking, tabHistory, tabMessage;
    private ImageView iconProfile, iconNotification, iconBooking, iconHistory, iconMessage;
    private TextView tvProfile, tvNotification, tvBooking, tvHistory, tvMessage;
    private View dotNotificationUnread, dotMessageUnread;

    public BottomNavFragment() {
    }

    public static BottomNavFragment newInstance(String currentTab, String roleMode) {
        return newInstance(currentTab, roleMode, false, false);
    }

    public static BottomNavFragment newInstance(String currentTab,
                                                String roleMode,
                                                boolean hasUnreadNotifications,
                                                boolean hasUnreadMessages) {
        BottomNavFragment fragment = new BottomNavFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CURRENT_TAB, currentTab);
        args.putString(ARG_ROLE_MODE, roleMode);
        args.putBoolean(ARG_HAS_UNREAD_NOTIFICATIONS, hasUnreadNotifications);
        args.putBoolean(ARG_HAS_UNREAD_MESSAGES, hasUnreadMessages);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bottom_nav, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            currentTab = getArguments().getString(ARG_CURRENT_TAB, TAB_BOOKING);
            roleMode = getArguments().getString(ARG_ROLE_MODE, ROLE_USER);
            hasUnreadNotifications = getArguments().getBoolean(ARG_HAS_UNREAD_NOTIFICATIONS, false);
            hasUnreadMessages = getArguments().getBoolean(ARG_HAS_UNREAD_MESSAGES, false);
        }

        tabProfile = view.findViewById(R.id.tabProfile);
        tabNotification = view.findViewById(R.id.tabNotification);
        tabBooking = view.findViewById(R.id.tabBooking);
        tabHistory = view.findViewById(R.id.tabHistory);
        tabMessage = view.findViewById(R.id.tabMessage);

        iconProfile = view.findViewById(R.id.iconProfile);
        iconNotification = view.findViewById(R.id.iconNotification);
        iconBooking = view.findViewById(R.id.iconBooking);
        iconHistory = view.findViewById(R.id.iconHistory);
        iconMessage = view.findViewById(R.id.iconMessage);

        tvProfile = view.findViewById(R.id.tvProfile);
        tvNotification = view.findViewById(R.id.tvNotification);
        tvBooking = view.findViewById(R.id.tvBooking);
        tvHistory = view.findViewById(R.id.tvHistory);
        tvMessage = view.findViewById(R.id.tvMessage);
        dotNotificationUnread = view.findViewById(R.id.dotNotificationUnread);
        dotMessageUnread = view.findViewById(R.id.dotMessageUnread);

        configureTabsForRole(view);
        applyLabels();
        updateSelectedTab();
        updateUnreadBadgeViews();

        tabProfile.setOnClickListener(v -> navigateTo(TAB_PROFILE));
        tabNotification.setOnClickListener(v -> navigateTo(TAB_NOTIFICATION));
        tabBooking.setOnClickListener(v -> navigateTo(TAB_BOOKING));
        tabHistory.setOnClickListener(v -> navigateTo(TAB_HISTORY));
        tabMessage.setOnClickListener(v -> navigateTo(TAB_MESSAGE));
    }

    private void navigateTo(String tab) {
        if (isAdminMode() && TAB_MESSAGE.equals(tab)) {
            return;
        }

        MainNavigator navigator = (MainNavigator) requireActivity();

        switch (tab) {
            case TAB_PROFILE:
                navigator.openProfile();
                break;
            case TAB_NOTIFICATION:
                navigator.openNotification();
                break;
            case TAB_BOOKING:
                navigator.openBooking();
                break;
            case TAB_HISTORY:
                navigator.openHistory();
                break;
            case TAB_MESSAGE:
                navigator.openMessage();
                break;
        }
    }

    private void applyLabels() {
        tvProfile.setText(R.string.nav_profile);
        tvNotification.setText(R.string.nav_notification);
        if (isAdminMode()) {
            tvBooking.setText(R.string.nav_admin_request);
            tvHistory.setText(R.string.nav_admin_promotion);
        } else {
            tvBooking.setText(isDriverMode() ? R.string.nav_driver_booking : R.string.nav_booking);
            tvHistory.setText(R.string.nav_history);
        }
        tvMessage.setText(R.string.nav_message);
    }

    private void configureTabsForRole(View rootView) {
        if (rootView instanceof LinearLayout) {
            ((LinearLayout) rootView).setWeightSum(isAdminMode() ? 4f : 5f);
        }

        tabMessage.setVisibility(isAdminMode() ? View.GONE : View.VISIBLE);
        iconBooking.setImageResource(isAdminMode() ? R.drawable.ic_policy : R.drawable.ic_logo);
        iconHistory.setImageResource(isAdminMode() ? R.drawable.ic_voucher : R.drawable.ic_history);
    }

    public void setUnreadBadges(boolean hasUnreadNotifications, boolean hasUnreadMessages) {
        this.hasUnreadNotifications = hasUnreadNotifications;
        this.hasUnreadMessages = hasUnreadMessages;

        Bundle arguments = getArguments();
        if (arguments != null) {
            arguments.putBoolean(ARG_HAS_UNREAD_NOTIFICATIONS, hasUnreadNotifications);
            arguments.putBoolean(ARG_HAS_UNREAD_MESSAGES, hasUnreadMessages);
        }

        updateUnreadBadgeViews();
    }

    private void updateUnreadBadgeViews() {
        if (dotNotificationUnread != null) {
            dotNotificationUnread.setVisibility(hasUnreadNotifications ? View.VISIBLE : View.GONE);
        }

        if (dotMessageUnread != null) {
            dotMessageUnread.setVisibility(!isAdminMode() && hasUnreadMessages ? View.VISIBLE : View.GONE);
        }
    }

    private void updateSelectedTab() {
        int blue = Color.parseColor("#0D5BD7");
        int black = Color.parseColor("#222222");

        resetTab(iconProfile, tvProfile, black);
        resetTab(iconNotification, tvNotification, black);
        resetTab(iconBooking, tvBooking, black);
        resetTab(iconHistory, tvHistory, black);
        resetTab(iconMessage, tvMessage, black);

        if (TAB_PROFILE.equals(currentTab)) {
            setSelected(iconProfile, tvProfile, blue);
        } else if (TAB_NOTIFICATION.equals(currentTab)) {
            setSelected(iconNotification, tvNotification, blue);
        } else if (TAB_BOOKING.equals(currentTab)) {
            setSelected(iconBooking, tvBooking, blue);
        } else if (TAB_HISTORY.equals(currentTab)) {
            setSelected(iconHistory, tvHistory, blue);
        } else if (TAB_MESSAGE.equals(currentTab)) {
            setSelected(iconMessage, tvMessage, blue);
        }
    }

    private void resetTab(ImageView icon, TextView textView, int color) {
        icon.setColorFilter(color);
        textView.setTextColor(color);
    }

    private void setSelected(ImageView icon, TextView textView, int color) {
        icon.setColorFilter(color);
        textView.setTextColor(color);
    }

    private boolean isDriverMode() {
        return ROLE_DRIVER.equals(roleMode);
    }

    private boolean isAdminMode() {
        return ROLE_ADMIN.equals(roleMode);
    }

    public interface MainNavigator {
        void openProfile();
        void openNotification();
        void openBooking();
        void openHistory();
        void openMessage();
    }
}
