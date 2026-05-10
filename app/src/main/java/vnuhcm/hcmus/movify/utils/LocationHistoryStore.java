package vnuhcm.hcmus.movify.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public final class LocationHistoryStore {

    public static final String KEY_PICKUP = "pickup_history";
    public static final String KEY_DESTINATION = "destination_history";

    private static final String PREFS_NAME = "location_history_prefs";
    private static final int MAX_ITEMS = 5;

    private LocationHistoryStore() {
    }

    @NonNull
    public static List<String> getHistory(@NonNull Context context, @NonNull String key) {
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String rawJson = prefs.getString(key, "[]");
        List<String> history = new ArrayList<>();

        try {
            JSONArray jsonArray = new JSONArray(rawJson);
            for (int index = 0; index < jsonArray.length(); index++) {
                String value = normalize(jsonArray.optString(index, ""));
                if (!TextUtils.isEmpty(value) && !containsIgnoreCase(history, value)) {
                    history.add(value);
                }
            }
        } catch (Exception ignored) {
            return history;
        }

        return history;
    }

    public static void saveLocation(@NonNull Context context,
                                    @NonNull String key,
                                    String location) {
        String normalizedLocation = normalize(location);
        if (TextUtils.isEmpty(normalizedLocation)) {
            return;
        }

        List<String> history = getHistory(context, key);
        for (int index = history.size() - 1; index >= 0; index--) {
            if (history.get(index).equalsIgnoreCase(normalizedLocation)) {
                history.remove(index);
            }
        }

        history.add(0, normalizedLocation);
        while (history.size() > MAX_ITEMS) {
            history.remove(history.size() - 1);
        }

        JSONArray jsonArray = new JSONArray();
        for (String item : history) {
            jsonArray.put(item);
        }

        context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(key, jsonArray.toString())
                .apply();
    }

    @NonNull
    private static String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.trim().replaceAll("\\s+", " ");
    }

    private static boolean containsIgnoreCase(List<String> values, String candidate) {
        for (String value : values) {
            if (value.equalsIgnoreCase(candidate)) {
                return true;
            }
        }

        return false;
    }
}
