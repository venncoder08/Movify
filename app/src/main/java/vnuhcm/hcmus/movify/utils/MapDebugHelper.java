package vnuhcm.hcmus.movify.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.GoogleMap;

public final class MapDebugHelper {

    private static final String TAG = "MapDebug";
    private static final String MAPS_API_KEY_META = "com.google.android.geo.API_KEY";

    private MapDebugHelper() {
    }

    public static void logMapReady(@NonNull Context context,
                                   @NonNull String screenName,
                                   @NonNull GoogleMap googleMap) {
        Log.i(TAG, screenName + ": onMapReady");
        logApiKeyStatus(context, screenName);

        googleMap.setOnMapLoadedCallback(() ->
                Log.i(TAG, screenName + ": onMapLoaded"));

        googleMap.setOnCameraIdleListener(() ->
                Log.d(TAG, screenName + ": camera idle"));
    }

    public static void logMissingMapFragment(@NonNull String screenName, int fragmentId) {
        Log.e(TAG, screenName + ": SupportMapFragment not found, fragmentId=" + fragmentId);
    }

    private static void logApiKeyStatus(@NonNull Context context, @NonNull String screenName) {
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                    context.getPackageName(),
                    PackageManager.GET_META_DATA
            );
            Bundle metaData = appInfo.metaData;
            String apiKey = metaData == null ? null : metaData.getString(MAPS_API_KEY_META);
            if (apiKey == null || apiKey.trim().isEmpty()) {
                Log.e(TAG, screenName + ": Google Maps API key is missing in manifest metadata");
                return;
            }

            int visibleChars = Math.min(6, apiKey.length());
            String maskedKey = apiKey.substring(0, visibleChars) + "...(" + apiKey.length() + " chars)";
            Log.i(TAG, screenName + ": Google Maps API key present: " + maskedKey);
        } catch (Exception exception) {
            Log.e(TAG, screenName + ": Unable to inspect manifest metadata", exception);
        }
    }
}
