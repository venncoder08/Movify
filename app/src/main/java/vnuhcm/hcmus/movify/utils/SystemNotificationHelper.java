package vnuhcm.hcmus.movify.utils;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import vnuhcm.hcmus.movify.R;
import vnuhcm.hcmus.movify.activities.HomeActivity;
import vnuhcm.hcmus.movify.fragments.BottomNavFragment;
import vnuhcm.hcmus.movify.models.Notification;

public final class SystemNotificationHelper {

    private static final String PREFS_NAME = "system_notification_prefs";
    private static final String KEY_LAST_NOTIFIED_ID = "last_notified_notification_id";
    private static final String CHANNEL_ID = "movify_updates_channel";
    private static final int NOTIFICATION_ID_BASE = 33052026;

    private SystemNotificationHelper() {
    }

    public static void notifyNewUnread(@NonNull Context context, @NonNull List<Notification> notifications) {
        if (notifications.isEmpty()) {
            return;
        }

        Context appContext = context.getApplicationContext();
        SharedPreferences prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int lastNotifiedId = prefs.getInt(KEY_LAST_NOTIFIED_ID, 0);
        int maxSeenId = lastNotifiedId;
        List<Notification> newUnreadNotifications = new ArrayList<>();

        for (Notification notification : notifications) {
            int notificationId = notification.getId();
            if (notificationId > maxSeenId) {
                maxSeenId = notificationId;
            }

            if (notification.isUnread() && notificationId > lastNotifiedId) {
                newUnreadNotifications.add(notification);
            }
        }

        if (newUnreadNotifications.isEmpty()) {
            if (maxSeenId > lastNotifiedId) {
                prefs.edit().putInt(KEY_LAST_NOTIFIED_ID, maxSeenId).apply();
            }
            return;
        }

        if (!canPostNotifications(appContext)) {
            return;
        }

        ensureChannel(appContext);
        newUnreadNotifications.sort(Comparator.comparingInt(Notification::getId));
        for (Notification notification : newUnreadNotifications) {
            postSystemNotification(appContext, notification);
        }

        if (maxSeenId > lastNotifiedId) {
            prefs.edit().putInt(KEY_LAST_NOTIFIED_ID, maxSeenId).apply();
        }
    }

    private static void postSystemNotification(@NonNull Context context, @NonNull Notification notification) {
        String title = TextUtils.isEmpty(notification.getTitle())
                ? context.getString(R.string.app_name)
                : notification.getTitle();
        String message = TextUtils.isEmpty(notification.getMessage())
                ? context.getString(R.string.system_notification_default_body)
                : notification.getMessage();

        Intent intent = new Intent(context, HomeActivity.class);
        intent.putExtra(HomeActivity.EXTRA_OPEN_TAB, BottomNavFragment.TAB_NOTIFICATION);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                notification.getId(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(resolveSmallIcon(notification))
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE);

        try {
            NotificationManagerCompat.from(context).notify(
                    NOTIFICATION_ID_BASE + notification.getId(),
                    builder.build()
            );
        } catch (SecurityException ignored) {
            // Runtime notification permission can be revoked at any time.
        }
    }

    private static int resolveSmallIcon(@NonNull Notification notification) {
        if (notification.isWarning()) {
            return R.drawable.ic_warning;
        }
        return R.drawable.ic_noti;
    }

    private static boolean canPostNotifications(@NonNull Context context) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return false;
        }

        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private static void ensureChannel(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        if (notificationManager == null) {
            return;
        }

        NotificationChannel existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID);
        if (existingChannel != null) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.system_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription(context.getString(R.string.system_notification_channel_description));
        notificationManager.createNotificationChannel(channel);
    }
}
