package vnuhcm.hcmus.movify.services;

import android.content.Context;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import vnuhcm.hcmus.movify.R;
import vnuhcm.hcmus.movify.models.History;
import vnuhcm.hcmus.movify.models.HistoryScreenData;

public final class HistoryService {

    private HistoryService() {
    }

    @NonNull
    public static HistoryScreenData fetchHistory(Context context) throws IOException, JSONException {
        String jwtToken = new AuthApiService(context).getJwtToken();
        if (jwtToken == null || jwtToken.isBlank()) {
            throw new IOException("No JWT token available");
        }

        String responseBody = BackendApiService.executeAuthenticatedGet(
                context,
                "api/history",
                jwtToken,
                new LinkedHashMap<>()
        );

        JSONObject root = new JSONObject(responseBody);
        JSONObject data = root.optJSONObject("data");
        List<History> items = new ArrayList<>();

        if (data == null) {
            return new HistoryScreenData(items, formatCurrency(0d), "0", formatDistance(0d), "Tháng này");
        }

        JSONArray historiesJson = data.optJSONArray("items");
        if (historiesJson != null) {
            for (int index = 0; index < historiesJson.length(); index++) {
                JSONObject item = historiesJson.optJSONObject(index);
                if (item == null) {
                    continue;
                }

                JSONObject driver = item.optJSONObject("driver");
                double driverRatePoint = driver == null ? 0d : driver.optDouble("ratePoint", 0d);
                String statusCode = item.optString("status", "Pending");
                String dateReference = firstNonEmpty(
                        item.optString("completedTime", ""),
                        item.optString("pickUpTime", ""),
                        item.optString("createdAt", "")
                );

                items.add(new History(
                        item.optInt("id", 0),
                        statusCode,
                        mapStatusLabel(statusCode),
                        formatDateTime(dateReference),
                        item.optString("pickUpLocation", "Chưa cập nhật"),
                        item.optString("destinationLocation", "Chưa cập nhật"),
                        driver == null ? "Chưa có tài xế" : driver.optString("fullName", "Chưa có tài xế"),
                        driver == null ? "Đang cập nhật" : driver.optString("vehicleRegistration", "Đang cập nhật"),
                        formatDuration(item),
                        formatDistance(item.optDouble("distance", 0d)),
                        formatCurrency(item.optDouble("totalAmount", 0d)),
                        buildRating(driverRatePoint),
                        mapTripIcon(item.optString("tripType", "Motor"))
                ));
            }
        }

        JSONObject summary = data.optJSONObject("summary");
        if (summary == null) {
            return new HistoryScreenData(items, formatCurrency(0d), "0", formatDistance(0d), "Thang nay");
        }

        return new HistoryScreenData(
                items,
                formatCurrency(summary.optDouble("totalAmount", 0d)),
                String.valueOf(summary.optInt("totalTrips", 0)),
                formatDistance(summary.optDouble("totalDistance", 0d)),
                summary.optString("periodLabel", "Tháng này")
        );
    }

    @NonNull
    private static String formatDuration(JSONObject item) {
        if (!item.has("duration") || item.isNull("duration")) {
            return "--";
        }

        double duration = item.optDouble("duration", 0d);
        return Math.round(duration) + " phút";
    }

    @NonNull
    private static String formatDateTime(String rawDate) {
        Date date = parseDate(rawDate);
        if (date == null) {
            return "Đang cập nhật";
        }

        return new SimpleDateFormat("dd/MM/yyyy | HH:mm", Locale.getDefault()).format(date);
    }

    @NonNull
    private static String formatCurrency(double amount) {
        NumberFormat formatter = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        formatter.setMaximumFractionDigits(0);
        formatter.setMinimumFractionDigits(0);
        return formatter.format(amount) + "đ";
    }

    @NonNull
    private static String formatDistance(double distance) {
        return String.format(Locale.US, "%.1f km", distance);
    }

    @NonNull
    private static String mapStatusLabel(String statusCode) {
        switch (statusCode) {
            case "Completed":
                return "Hoàn thành";
            case "Cancelled":
                return "Đã hủy";
            case "Accepted":
                return "Đã nhận";
            case "InProgress":
                return "Đang di chuyển";
            default:
                return "Đang xử lý";
        }
    }

    private static int mapTripIcon(String tripType) {
        return "Taxi".equalsIgnoreCase(tripType) ? R.drawable.ic_car : R.drawable.ic_motorbike;
    }

    @NonNull
    private static String buildRating(double driverRatePoint) {
        if (driverRatePoint <= 0d) {
            return "";
        }

        return "Rating " + String.format(Locale.US, "%.1f", driverRatePoint);
    }

    @NonNull
    private static String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return "";
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
