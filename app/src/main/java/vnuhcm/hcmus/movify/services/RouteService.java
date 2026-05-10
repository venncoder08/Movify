package vnuhcm.hcmus.movify.services;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import vnuhcm.hcmus.movify.models.PlaceDetails;
import vnuhcm.hcmus.movify.models.RoutePreview;

public final class RouteService {

    private RouteService() {
    }

    @NonNull
    public static RoutePreview fetchRoutePreview(Context context, String pickup, String destination)
            throws IOException, JSONException {
        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("pickup", pickup);
        queryParams.put("destination", destination);

        String responseBody = BackendApiService.executeGet(context, "api/routes/preview", queryParams);
        JSONObject root = new JSONObject(responseBody);
        JSONObject pickupJson = root.optJSONObject("pickup");
        JSONObject destinationJson = root.optJSONObject("destination");
        JSONArray routePointsJson = root.optJSONArray("routePoints");

        if (pickupJson == null || destinationJson == null || routePointsJson == null) {
            throw new IOException("Máy chủ không trả về dữ liệu tuyến hợp lệ");
        }

        List<LatLng> routePoints = new ArrayList<>();
        for (int i = 0; i < routePointsJson.length(); i++) {
            JSONObject pointJson = routePointsJson.optJSONObject(i);
            if (pointJson == null) {
                continue;
            }

            double lat = pointJson.optDouble("lat", Double.NaN);
            double lng = pointJson.optDouble("lng", Double.NaN);
            if (!Double.isNaN(lat) && !Double.isNaN(lng)) {
                routePoints.add(new LatLng(lat, lng));
            }
        }

        PlaceDetails pickupDetails = PlaceService.parsePlaceDetails(pickupJson);
        PlaceDetails destinationDetails = PlaceService.parsePlaceDetails(destinationJson);

        return new RoutePreview(
                pickupDetails,
                destinationDetails,
                routePoints,
                root.optInt("distanceMeters", 0),
                root.optDouble("durationSeconds", 0d)
        );
    }
}
