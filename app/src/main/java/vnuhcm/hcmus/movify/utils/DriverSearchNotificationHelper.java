package vnuhcm.hcmus.movify.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import vnuhcm.hcmus.movify.R;
import vnuhcm.hcmus.movify.activities.HomeActivity;

public final class DriverSearchNotificationHelper {

    private static final String CHANNEL_ID = "driver_search_channel";
    private static final int NOTIFICATION_ID = 22052026;
    private static final AtomicBoolean SEARCHING = new AtomicBoolean(false);
    private static volatile long searchStartedAtElapsedRealtime = 0L;

    private DriverSearchNotificationHelper() {
    }

    public static void start(@NonNull Context context) {
        SEARCHING.set(true);
        searchStartedAtElapsedRealtime = SystemClock.elapsedRealtime();
        postNotification(context, false);
    }

    public static void update(@NonNull Context context) {
        if (!SEARCHING.get()) {
            return;
        }
        postNotification(context, false);
    }

    public static void notifyAppExited(@NonNull Context context) {
        if (!SEARCHING.get()) {
            return;
        }
        postNotification(context, true);
    }

    public static void stop(@NonNull Context context) {
        SEARCHING.set(false);
        NotificationManagerCompat.from(context.getApplicationContext()).cancel(NOTIFICATION_ID);
    }

    public static void notifyDriverAccepted(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        SEARCHING.set(false);
        ensureChannel(appContext);

        Intent intent = new Intent(appContext, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                appContext,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_noti)
                .setContentTitle("Tài xế đã nhận chuyến")
                .setContentText("Tài xế đang di chuyển đến điểm đón của bạn.")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Tài xế đang di chuyển đến điểm đón của bạn."))
                .setContentIntent(contentIntent)
                .setOngoing(false)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_STATUS);

        NotificationManagerCompat.from(appContext).notify(NOTIFICATION_ID, builder.build());
    }

    public static boolean isSearching() {
        return SEARCHING.get();
    }

    public static long getElapsedMillis() {
        if (!SEARCHING.get() || searchStartedAtElapsedRealtime <= 0L) {
            return 0L;
        }
        return Math.max(0L, SystemClock.elapsedRealtime() - searchStartedAtElapsedRealtime);
    }

    private static void postNotification(@NonNull Context context, boolean appExited) {
        Context appContext = context.getApplicationContext();
        ensureChannel(appContext);

        long elapsedMillis = getElapsedMillis();
        String elapsedLabel = formatElapsed(elapsedMillis);
        String title = appContext.getString(R.string.driver_search_notification_title);
        String body = appExited
                ? appContext.getString(R.string.driver_search_notification_exit_body, elapsedLabel)
                : appContext.getString(R.string.driver_search_notification_body, elapsedLabel);

        Intent intent = new Intent(appContext, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                appContext,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_noti)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setAutoCancel(false)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_STATUS);

        NotificationManagerCompat.from(appContext).notify(NOTIFICATION_ID, builder.build());
    }

    private static void ensureChannel(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        if (notificationManager == null) {
            return;
        }

        NotificationChannel channel = notificationManager.getNotificationChannel(CHANNEL_ID);
        if (channel != null) {
            return;
        }

        NotificationChannel newChannel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.driver_search_channel_name),
                NotificationManager.IMPORTANCE_HIGH
        );
        newChannel.setDescription(context.getString(R.string.driver_search_channel_description));
        notificationManager.createNotificationChannel(newChannel);
    }

    @NonNull
    private static String formatElapsed(long elapsedMillis) {
        long totalSeconds = elapsedMillis / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }
}
