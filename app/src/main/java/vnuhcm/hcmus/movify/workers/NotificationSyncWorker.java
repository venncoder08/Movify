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
import vnuhcm.hcmus.movify.utils.SystemNotificationHelper;

public class NotificationSyncWorker extends Worker {

    public NotificationSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context appContext = getApplicationContext();
        AuthApiService authApiService = new AuthApiService(appContext);
        if (!authApiService.isLoggedIn()) {
            return Result.success();
        }

        try {
            NotificationFeed feed = NotificationService.fetchNotifications(appContext);
            SystemNotificationHelper.notifyNewUnread(appContext, feed.getNotifications());
            return Result.success();
        } catch (JSONException exception) {
            return Result.failure();
        } catch (IOException exception) {
            return Result.retry();
        } catch (SecurityException exception) {
            return Result.success();
        }
    }
}
