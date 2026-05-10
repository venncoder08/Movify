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
    static PlaceDetails parsePlaceDetails(JSONObject jsonObject) throws JSONException {
        double lat = jsonObject.getDouble("lat");
        double lng = jsonObject.getDouble("lng");
        String label = jsonObject.optString("label", String.format(Locale.US, "%f,%f", lat, lng));
        return new PlaceDetails(label, new LatLng(lat, lng));
    }
}
