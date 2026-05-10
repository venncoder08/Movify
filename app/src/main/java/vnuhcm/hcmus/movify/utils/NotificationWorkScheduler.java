package vnuhcm.hcmus.movify.utils;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

import vnuhcm.hcmus.movify.workers.NotificationSyncWorker;

public final class NotificationWorkScheduler {

    private static final String PERIODIC_WORK_NAME = "movify_notification_sync";
    private static final String IMMEDIATE_WORK_NAME = "movify_notification_sync_now";
    private static final long PERIODIC_INTERVAL_MINUTES = 15L;

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

    public static void cancel(@NonNull Context context) {
        WorkManager workManager = WorkManager.getInstance(context.getApplicationContext());
        workManager.cancelUniqueWork(PERIODIC_WORK_NAME);
        workManager.cancelUniqueWork(IMMEDIATE_WORK_NAME);
    }
}
