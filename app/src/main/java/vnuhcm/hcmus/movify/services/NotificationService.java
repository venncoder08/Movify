package vnuhcm.hcmus.movify.services;

import android.content.Context;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import vnuhcm.hcmus.movify.R;
import vnuhcm.hcmus.movify.models.Notification;
import vnuhcm.hcmus.movify.models.NotificationFeed;

public final class NotificationService {

    private NotificationService() {
    }

    @NonNull
    public static NotificationFeed fetchNotifications(Context context) throws IOException, JSONException {
        AuthApiService authApiService = new AuthApiService(context);
        String jwtToken = authApiService.getJwtToken();

        if (jwtToken == null || jwtToken.isBlank()) {
            throw new IOException("Không tìm thấy mã phiên đăng nhập");
        }

        String responseBody = BackendApiService.executeAuthenticatedGet(
                context,
                "api/notifications",
                jwtToken,
                new LinkedHashMap<>()
        );

        JSONObject root = new JSONObject(responseBody);
        JSONObject data = root.optJSONObject("data");
        List<Notification> notifications = new ArrayList<>();

        if (data == null) {
            return new NotificationFeed(notifications, 0);
        }

        JSONArray items = data.optJSONArray("items");
        if (items != null) {
            for (int index = 0; index < items.length(); index++) {
                JSONObject item = items.optJSONObject(index);
                if (item == null) {
                    continue;
                }

                boolean isRead = item.optBoolean("isRead", false);
                notifications.add(new Notification(
                        item.optInt("id", 0),
                        resolveIcon(item.optString("iconKey", ""), item.optBoolean("isWarning", false)),
                        item.optString("title", ""),
                        item.optString("body", ""),
                        formatRelativeTime(item.optString("createdAt", "")),
                        item.optString("category", "Thông báo"),
                        !isRead,
                        item.optBoolean("isWarning", false)
                ));
            }
        }

        return new NotificationFeed(notifications, data.optInt("unreadCount", 0));
    }

    public static void markNotificationAsRead(Context context, int notificationId) throws IOException {
        String jwtToken = requireJwtToken(context);
        BackendApiService.executeAuthenticatedPatch(
                context,
                "api/notifications/" + notificationId + "/read",
                jwtToken,
                new JSONObject()
        );
    }

    public static void markAllNotificationsAsRead(Context context) throws IOException {
        String jwtToken = requireJwtToken(context);
        BackendApiService.executeAuthenticatedPatch(
                context,
                "api/notifications/read-all",
                jwtToken,
                new JSONObject()
        );
    }

    @NonNull
    private static String requireJwtToken(Context context) throws IOException {
        String jwtToken = new AuthApiService(context).getJwtToken();
        if (jwtToken == null || jwtToken.isBlank()) {
            throw new IOException("Không tìm thấy mã phiên đăng nhập");
        }
        return jwtToken;
    }

    private static int resolveIcon(String iconKey, boolean isWarning) {
        if (isWarning) {
            return R.drawable.ic_flag;
        }

        switch (iconKey) {
            case "payment":
                return R.drawable.ic_card;
            case "trip":
                return R.drawable.ic_flag;
            case "promotion":
                return R.drawable.ic_voucher;
            case "warning":
                return R.drawable.ic_warning;
            default:
                return R.drawable.ic_noti;
        }
    }

    @NonNull
    private static String formatRelativeTime(String rawDate) {
        Date date = parseDate(rawDate);
        if (date == null) {
            return "Vừa cập nhật";
        }

        long diffMillis = Math.max(0L, System.currentTimeMillis() - date.getTime());
        long minutes = diffMillis / (60L * 1000L);
        if (minutes < 1L) {
            return "Vừa xong";
        }
        if (minutes < 60L) {
            return minutes + " phút trước";
        }

        long hours = minutes / 60L;
        if (hours < 24L) {
            return hours + " giờ trước";
        }

        long days = hours / 24L;
        if (days < 7L) {
            return days + " ngày trước";
        }

        return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(date);
    }

    private static Date parseDate(String rawDate) {
        if (rawDate == null || rawDate.trim().isEmpty()) {
            return null;
        }

        String[] patterns = {
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSSX",
                "yyyy-MM-dd'T'HH:mm:ssX"
        };

        for (String pattern : patterns) {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat(pattern, Locale.US);
                return dateFormat.parse(rawDate);
            } catch (ParseException ignored) {
                // Try next format.
            }
        }

        return null;
    }
}
