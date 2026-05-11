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
import java.util.Locale;
import java.util.Map;

import vnuhcm.hcmus.movify.models.DestinationRecommendation;
import vnuhcm.hcmus.movify.models.PlaceDetails;

public final class PlaceService {

    private PlaceService() {
    }

    @NonNull
    public static List<String> fetchAutocompleteSuggestions(Context context, String query)
            throws IOException, JSONException {
        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("q", query);

        String responseBody = BackendApiService.executeGet(context, "api/places/autocomplete", queryParams);
        JSONObject root = new JSONObject(responseBody);
        JSONArray suggestionsJson = root.optJSONArray("suggestions");
        List<String> suggestions = new ArrayList<>();
        if (suggestionsJson == null) {
            return suggestions;
        }

        for (int i = 0; i < suggestionsJson.length(); i++) {
            String suggestion = suggestionsJson.optString(i, "").trim();
            if (!suggestion.isEmpty()) {
                suggestions.add(suggestion);
            }
        }

        return suggestions;
    }

    @NonNull
    public static PlaceDetails fetchPlaceDetails(Context context, String query)
            throws IOException, JSONException {
        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("q", query);

        String responseBody = BackendApiService.executeGet(context, "api/places/geocode", queryParams);
        JSONObject root = new JSONObject(responseBody);
        JSONObject locationJson = root.optJSONObject("location");
        if (locationJson == null) {
            throw new IOException("Máy chủ không trả về vị trí");
        }

        return parsePlaceDetails(locationJson);
    }

    @NonNull
    public static List<DestinationRecommendation> fetchDestinationRecommendations(Context context,
                                                                                  String query,
                                                                                  LatLng origin)
            throws IOException, JSONException {
        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("q", query);
        queryParams.put("lat", String.valueOf(origin.latitude));
        queryParams.put("lng", String.valueOf(origin.longitude));
        queryParams.put("radiusMeters", "5000");
        queryParams.put("limit", "10");

        String responseBody = BackendApiService.executeGet(context, "api/places/recommendations", queryParams);
        JSONObject root = new JSONObject(responseBody);
        JSONObject dataJson = root.optJSONObject("data");
        JSONArray recommendationsJson = dataJson != null
                ? dataJson.optJSONArray("recommendations")
                : root.optJSONArray("recommendations");
        List<DestinationRecommendation> recommendations = new ArrayList<>();
        if (recommendationsJson == null) {
            return recommendations;
        }

        for (int i = 0; i < recommendationsJson.length(); i++) {
            JSONObject itemJson = recommendationsJson.optJSONObject(i);
            if (itemJson != null) {
                recommendations.add(parseDestinationRecommendation(itemJson));
            }
        }

        return recommendations;
    }

    @NonNull
    static PlaceDetails parsePlaceDetails(JSONObject jsonObject) throws JSONException {
        double lat = jsonObject.getDouble("lat");
        double lng = jsonObject.getDouble("lng");
        String label = jsonObject.optString("label", String.format(Locale.US, "%f,%f", lat, lng));
        return new PlaceDetails(label, new LatLng(lat, lng));
    }

    @NonNull
    private static DestinationRecommendation parseDestinationRecommendation(JSONObject jsonObject) throws JSONException {
        return new DestinationRecommendation(
                jsonObject.optString("placeId", ""),
                jsonObject.optString("name", "Địa điểm phù hợp"),
                jsonObject.optString("label", ""),
                jsonObject.optString("address", ""),
                jsonObject.optString("categoryLabel", "Dịch vụ gần đây"),
                jsonObject.optString("reason", ""),
                jsonObject.getDouble("lat"),
                jsonObject.getDouble("lng"),
                jsonObject.optDouble("distanceMeters", 0d),
                jsonObject.optDouble("similarityScore", 0d),
                jsonObject.optDouble("recommendationScore", 0d)
        );
    }
}
