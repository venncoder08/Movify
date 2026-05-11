package vnuhcm.hcmus.movify.utils;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

import vnuhcm.hcmus.movify.workers.NotificationSyncWorker;

public final class NotificationWorkScheduler {

    public static final String KEY_BACKGROUND_WATCH_TOKEN = "background_watch_token";
    public static final String KEY_BACKGROUND_WATCH_REMAINING_RUNS = "background_watch_remaining_runs";
    public static final String KEY_BACKGROUND_WATCH_INTERVAL_SECONDS = "background_watch_interval_seconds";

    private static final String PREFS_NAME = "notification_work_scheduler";
    private static final String PREF_ACTIVE_BACKGROUND_WATCH_TOKEN = "active_background_watch_token";
    private static final String PERIODIC_WORK_NAME = "movify_notification_sync";
    private static final String IMMEDIATE_WORK_NAME = "movify_notification_sync_now";
    private static final String BACKGROUND_WATCH_TAG = "movify_notification_background_watch";
    private static final long PERIODIC_INTERVAL_MINUTES = 15L;
    private static final int BACKGROUND_WATCH_RUNS = 24;
    private static final int BACKGROUND_WATCH_INTERVAL_SECONDS = 15;

    private NotificationWorkScheduler() {
    }

    public static void schedule(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest periodicRequest = new PeriodicWorkRequest.Builder(
                NotificationSyncWorker.class,
                PERIODIC_INTERVAL_MINUTES,
                TimeUnit.MINUTES
        )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30L, TimeUnit.SECONDS)
                .build();

        WorkManager workManager = WorkManager.getInstance(appContext);
        workManager.enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicRequest
        );
        syncNow(appContext);
    }

    public static void syncNow(@NonNull Context context) {
        OneTimeWorkRequest immediateRequest = new OneTimeWorkRequest.Builder(NotificationSyncWorker.class)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                .build();

        WorkManager.getInstance(context.getApplicationContext()).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                immediateRequest
        );
    }

    public static void startBackgroundWatch(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        long token = System.currentTimeMillis();
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong(PREF_ACTIVE_BACKGROUND_WATCH_TOKEN, token)
                .apply();

        enqueueBackgroundWatch(
                appContext,
                token,
                BACKGROUND_WATCH_RUNS,
                BACKGROUND_WATCH_INTERVAL_SECONDS,
                0L
        );
    }

    public static void scheduleNextBackgroundWatch(@NonNull Context context,
                                                   long token,
                                                   int remainingRuns,
                                                   int intervalSeconds) {
        Context appContext = context.getApplicationContext();
        if (remainingRuns <= 0 || !isBackgroundWatchCurrent(appContext, token)) {
            return;
        }

        int safeIntervalSeconds = intervalSeconds > 0
                ? intervalSeconds
                : BACKGROUND_WATCH_INTERVAL_SECONDS;
        enqueueBackgroundWatch(appContext, token, remainingRuns, safeIntervalSeconds, safeIntervalSeconds);
    }

    public static boolean isBackgroundWatchCurrent(@NonNull Context context, long token) {
        if (token <= 0L) {
            return true;
        }

        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getLong(PREF_ACTIVE_BACKGROUND_WATCH_TOKEN, 0L) == token;
    }

    public static void cancel(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(PREF_ACTIVE_BACKGROUND_WATCH_TOKEN)
                .apply();

        WorkManager workManager = WorkManager.getInstance(appContext);
        workManager.cancelUniqueWork(PERIODIC_WORK_NAME);
        workManager.cancelUniqueWork(IMMEDIATE_WORK_NAME);
        workManager.cancelAllWorkByTag(BACKGROUND_WATCH_TAG);
    }

    private static void enqueueBackgroundWatch(@NonNull Context appContext,
                                               long token,
                                               int remainingRuns,
                                               int intervalSeconds,
                                               long initialDelaySeconds) {
        Data inputData = new Data.Builder()
                .putLong(KEY_BACKGROUND_WATCH_TOKEN, token)
                .putInt(KEY_BACKGROUND_WATCH_REMAINING_RUNS, remainingRuns)
                .putInt(KEY_BACKGROUND_WATCH_INTERVAL_SECONDS, intervalSeconds)
                .build();

        OneTimeWorkRequest.Builder requestBuilder = new OneTimeWorkRequest.Builder(NotificationSyncWorker.class)
                .setInputData(inputData)
                .addTag(BACKGROUND_WATCH_TAG)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build());

        if (initialDelaySeconds > 0L) {
            requestBuilder.setInitialDelay(initialDelaySeconds, TimeUnit.SECONDS);
        }

        WorkManager.getInstance(appContext).enqueue(requestBuilder.build());
    }
}
