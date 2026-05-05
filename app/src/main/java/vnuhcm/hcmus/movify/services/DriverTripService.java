package vnuhcm.hcmus.movify.services;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import vnuhcm.hcmus.movify.models.Trip;

public final class DriverTripService {

    private static final String PREFS_NAME = "auth_prefs";
    private static final String KEY_JWT_TOKEN = "jwt_token";

    private DriverTripService() {
    }

    /**
     * Get JWT token from SharedPreferences
     */
    private static String getJwtToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_JWT_TOKEN, "");
    }

    /**
     * Fetch nearby available trips for the driver
     *
     * @param context    Application context
     * @param driverLat  Driver's current latitude
     * @param driverLng  Driver's current longitude
     * @param radius     Search radius in km (default: 5)
     * @return List of nearby Trip objects
     * @throws IOException   If API call fails
     * @throws JSONException If response parsing fails
     */
    @NonNull
    public static List<Trip> getNearbyTrips(Context context, double driverLat, double driverLng, double radius)
            throws IOException, JSONException {
        String jwtToken = getJwtToken(context);
        
        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("lat", String.valueOf(driverLat));
        queryParams.put("lng", String.valueOf(driverLng));
        queryParams.put("radius", String.valueOf(radius));

        String responseBody = BackendApiService.executeAuthenticatedGet(context, "api/driver/nearby-trips", jwtToken, queryParams);
        JSONObject root = new JSONObject(responseBody);
        JSONObject dataJson = root.optJSONObject("data");

        if (dataJson == null) {
            throw new IOException("Backend không trả về dữ liệu trips hợp lệ");
        }

        JSONArray tripsJson = dataJson.optJSONArray("trips");
        List<Trip> trips = new ArrayList<>();

        if (tripsJson != null) {
            for (int i = 0; i < tripsJson.length(); i++) {
                JSONObject tripJson = tripsJson.optJSONObject(i);
                if (tripJson != null) {
                    Trip trip = parseTripFromJson(tripJson);
                    if (trip != null) {
                        trips.add(trip);
                    }
                }
            }
        }

        return trips;
    }

    /**
     * Accept a trip
     *
     * @param context Application context
     * @param tripId  Trip ID to accept
     * @return Accepted Trip object
     * @throws IOException   If API call fails
     * @throws JSONException If response parsing fails
     */
    @NonNull
    public static Trip acceptTrip(Context context, int tripId)
            throws IOException, JSONException {
        String jwtToken = getJwtToken(context);
        
        JSONObject requestBody = new JSONObject();
        requestBody.put("tripId", tripId);

        String responseBody = BackendApiService.executeAuthenticatedPost(context, "api/driver/accept-trip", jwtToken, requestBody);
        JSONObject root = new JSONObject(responseBody);
        JSONObject dataJson = root.optJSONObject("data");

        if (dataJson == null) {
            throw new IOException("Backend không trả về dữ liệu trip hợp lệ");
        }

        JSONObject tripJson = dataJson.optJSONObject("trip");
        if (tripJson == null) {
            throw new IOException("Backend không trả về thông tin trip");
        }

        Trip trip = parseTripFromJson(tripJson);
        if (trip == null) {
            throw new IOException("Không thể parse trip từ response");
        }

        return trip;
    }

    /**
     * Parse Trip object from JSON
     */
    private static Trip parseTripFromJson(JSONObject json) {
        try {
            int id = json.getInt("id");
            String pickupAddress = json.getString("pickupAddress");
            double pickupLat = json.getDouble("pickupLat");
            double pickupLng = json.getDouble("pickupLng");
            String destinationAddress = json.getString("destinationAddress");
            double destinationLat = json.getDouble("destinationLat");
            double destinationLng = json.getDouble("destinationLng");
            double fare = json.getDouble("fare");
            double distance = json.getDouble("distance");
            String status = json.getString("status");

            return new Trip(
                    String.valueOf(id),
                    pickupLat,
                    pickupLng,
                    pickupAddress,
                    destinationLat,
                    destinationLng,
                    destinationAddress,
                    fare,
                    status,
                    distance
            );
        } catch (JSONException e) {
            return null;
        }
    }
}
