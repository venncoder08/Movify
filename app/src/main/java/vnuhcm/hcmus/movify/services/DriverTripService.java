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

import vnuhcm.hcmus.movify.models.AccountProfile;
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
            throw new IOException("Máy chủ không trả về dữ liệu chuyến hợp lệ");
        }

        JSONArray tripsJson = dataJson.optJSONArray("trips");
        String driverVehicleType = normalizeTripType(dataJson.optString("vehicleType", ""));
        if (driverVehicleType.isEmpty()) {
            driverVehicleType = resolveDriverVehicleType(context);
        }

        List<Trip> trips = new ArrayList<>();

        if (tripsJson != null) {
            for (int i = 0; i < tripsJson.length(); i++) {
                JSONObject tripJson = tripsJson.optJSONObject(i);
                if (tripJson != null) {
                    Trip trip = parseTripFromJson(tripJson);
                    if (trip != null && matchesVehicleType(driverVehicleType, trip.getTripType())) {
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
            throw new IOException("Máy chủ không trả về dữ liệu chuyến hợp lệ");
        }

        JSONObject tripJson = dataJson.optJSONObject("trip");
        if (tripJson == null) {
            throw new IOException("Máy chủ không trả về thông tin chuyến");
        }

        Trip trip = parseTripFromJson(tripJson);
        if (trip == null) {
            throw new IOException("Không thể đọc dữ liệu chuyến từ phản hồi");
        }

        return trip;
    }

    /**
     * Parse Trip object from JSON
     */
    private static Trip parseTripFromJson(JSONObject json) {
        try {
            int id = json.getInt("id");
            String pickupAddress = json.optString("pickupAddress", "");
            double pickupLat = json.optDouble("pickupLat", 0d);
            double pickupLng = json.optDouble("pickupLng", 0d);
            String destinationAddress = json.optString("destinationAddress", "");
            double destinationLat = json.optDouble("destinationLat", 0d);
            double destinationLng = json.optDouble("destinationLng", 0d);
            double fare = json.optDouble("fare", 0d);
            double distance = json.optDouble("distance", 0d);
            String status = json.optString("status", "");
            String tripType = json.optString("tripType", "");

            Trip trip = new Trip(
                    String.valueOf(id),
                    pickupLat,
                    pickupLng,
                    pickupAddress,
                    destinationLat,
                    destinationLng,
                    destinationAddress,
                    fare,
                    status,
                    distance,
                    tripType
            );
            applyPickupConfirmation(trip, json);
            applyDriverLocation(trip, json);
            applyDriverInfo(trip, json);
            return trip;
        } catch (JSONException e) {
            return null;
        }
    }

    private static void applyPickupConfirmation(@NonNull Trip trip, @NonNull JSONObject tripJson) {
        JSONObject pickupConfirmation = tripJson.optJSONObject("pickupConfirmation");
        if (pickupConfirmation == null) {
            return;
        }

        trip.setDriverPickupConfirmed(pickupConfirmation.optBoolean("driverConfirmed", false));
        trip.setPassengerPickupConfirmed(pickupConfirmation.optBoolean("passengerConfirmed", false));
    }

    private static void applyDriverLocation(@NonNull Trip trip, @NonNull JSONObject tripJson) {
        JSONObject driverLocation = tripJson.optJSONObject("driverLocation");
        if (driverLocation == null) {
            return;
        }

        if (!driverLocation.isNull("lat") && !driverLocation.isNull("lng")) {
            trip.setDriverCurrentLat(driverLocation.optDouble("lat", 0d));
            trip.setDriverCurrentLng(driverLocation.optDouble("lng", 0d));
        }
        trip.setDriverLocationUpdatedAt(driverLocation.optString("updatedAt", ""));
    }

    private static void applyDriverInfo(@NonNull Trip trip, @NonNull JSONObject tripJson) {
        JSONObject driver = tripJson.optJSONObject("driver");
        if (driver != null) {
            trip.setDriverId(driver.optInt("id", 0));
            trip.setDriverFullName(driver.optString("fullName", ""));
            trip.setDriverPhoneNumber(driver.optString("phoneNumber", ""));
        }

        JSONObject passenger = tripJson.optJSONObject("passenger");
        if (passenger == null) {
            return;
        }

        trip.setPassengerId(passenger.optInt("id", 0));
        trip.setPassengerFullName(passenger.optString("fullName", ""));
        trip.setPassengerPhoneNumber(passenger.optString("phoneNumber", ""));
    }

    @NonNull
    private static String resolveDriverVehicleType(Context context) {
        try {
            AccountProfile profile = new AuthApiService(context).fetchCurrentUserProfile();
            return normalizeTripType(profile.getVehicleType());
        } catch (Exception ignored) {
            return "";
        }
    }

    private static boolean matchesVehicleType(String driverVehicleType, String tripType) {
        String normalizedTripType = normalizeTripType(tripType);
        return !driverVehicleType.isEmpty()
                && !normalizedTripType.isEmpty()
                && driverVehicleType.equals(normalizedTripType);
    }

    @NonNull
    private static String normalizeTripType(String value) {
        if ("Taxi".equalsIgnoreCase(value) || "car".equalsIgnoreCase(value)) {
            return "Taxi";
        }

        if ("Motor".equalsIgnoreCase(value) || "motorbike".equalsIgnoreCase(value)) {
            return "Motor";
        }

        return "";
    }
}
