package vnuhcm.hcmus.movify;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vnuhcm.hcmus.movify.utils.NotificationWorkScheduler;

public class MovifyApplication extends Application implements Application.ActivityLifecycleCallbacks {

    private static volatile boolean appInForeground = false;
    private int startedActivities = 0;

    public static boolean isAppInForeground() {
        return appInForeground;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        startedActivities += 1;
        appInForeground = true;
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        startedActivities = Math.max(0, startedActivities - 1);
        if (startedActivities == 0 && !activity.isChangingConfigurations()) {
            appInForeground = false;
            NotificationWorkScheduler.startBackgroundWatch(getApplicationContext());
        }
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
    }
}
