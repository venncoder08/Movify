package vnuhcm.hcmus.movify.workers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONException;

import java.io.IOException;

import vnuhcm.hcmus.movify.models.NotificationFeed;
import vnuhcm.hcmus.movify.services.AuthApiService;
import vnuhcm.hcmus.movify.services.NotificationService;
import vnuhcm.hcmus.movify.utils.NotificationWorkScheduler;
import vnuhcm.hcmus.movify.utils.SystemNotificationHelper;

public class NotificationSyncWorker extends Worker {

    public NotificationSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context appContext = getApplicationContext();
        long backgroundWatchToken = getInputData().getLong(
                NotificationWorkScheduler.KEY_BACKGROUND_WATCH_TOKEN,
                0L
        );
        if (!NotificationWorkScheduler.isBackgroundWatchCurrent(appContext, backgroundWatchToken)) {
            return Result.success();
        }

        AuthApiService authApiService = new AuthApiService(appContext);
        if (!authApiService.isLoggedIn()) {
            return Result.success();
        }

        try {
            NotificationFeed feed = NotificationService.fetchNotifications(appContext);
            SystemNotificationHelper.notifyNewUnread(appContext, feed.getNotifications());
            scheduleNextBackgroundWatchIfNeeded(appContext, backgroundWatchToken);
            return Result.success();
        } catch (JSONException exception) {
            return Result.failure();
        } catch (IOException exception) {
            return Result.retry();
        } catch (SecurityException exception) {
            return Result.success();
        }
    }

    private void scheduleNextBackgroundWatchIfNeeded(@NonNull Context appContext, long backgroundWatchToken) {
        if (backgroundWatchToken <= 0L) {
            return;
        }

        int remainingRuns = getInputData().getInt(
                NotificationWorkScheduler.KEY_BACKGROUND_WATCH_REMAINING_RUNS,
                0
        );
        int intervalSeconds = getInputData().getInt(
                NotificationWorkScheduler.KEY_BACKGROUND_WATCH_INTERVAL_SECONDS,
                0
        );

        NotificationWorkScheduler.scheduleNextBackgroundWatch(
                appContext,
                backgroundWatchToken,
                remainingRuns - 1,
                intervalSeconds
        );
    }
}
