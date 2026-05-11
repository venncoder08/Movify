package vnuhcm.hcmus.movify.services;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import vnuhcm.hcmus.movify.models.Trip;

/**
 * Handles passenger-side trip lifecycle: create, cancel.
 */
public final class TripService {

    private static final String PREFS_NAME  = "auth_prefs";
    private static final String KEY_JWT     = "jwt_token";

    private TripService() {
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Create a new trip request on the backend.
     *
     * @param context         Application context
     * @param pickupAddress   Human-readable pickup address
     * @param pickupLat       Pickup latitude  (0 if unknown)
     * @param pickupLng       Pickup longitude (0 if unknown)
     * @param destAddress     Human-readable destination address
     * @param destLat         Destination latitude  (0 if unknown)
     * @param destLng         Destination longitude (0 if unknown)
     * @param vehicleType     "motorbike" or "car"
     * @param paymentMethod   "cash" or "bank_transfer"
     * @param promotionId     Promotion id, or null if none
     * @param estimatedFare   Fare calculated on the client (informational)
     * @param distanceMeters  Distance in metres from RoutePreview
     * @return The created {@link Trip} with its backend-assigned id and status
     * @throws IOException   Network or HTTP error
     * @throws JSONException Unexpected response format
     */
    @NonNull
    public static Trip createTrip(@NonNull Context context,
                                  @NonNull String pickupAddress,
                                  double pickupLat,
                                  double pickupLng,
                                  @NonNull String destAddress,
                                  double destLat,
                                  double destLng,
                                  @NonNull String vehicleType,
                                  @NonNull String paymentMethod,
                                  int promotionId,          // -1 means no promotion
                                  double estimatedFare,
                                  int distanceMeters)
            throws IOException, JSONException {

        String jwt = getJwt(context);

        JSONObject body = new JSONObject();
        body.put("pickupAddress",      pickupAddress);
        body.put("pickupLat",          pickupLat);
        body.put("pickupLng",          pickupLng);
        body.put("destinationAddress", destAddress);
        body.put("destinationLat",     destLat);
        body.put("destinationLng",     destLng);
        body.put("vehicleType",        vehicleType);
        body.put("paymentMethod",      paymentMethod);
        body.put("estimatedFare",      estimatedFare);
        body.put("distanceMeters",     distanceMeters);
        if (promotionId > 0) {
            body.put("promotionId", promotionId);
        }

        String responseBody = BackendApiService.executeAuthenticatedPost(
                context, "api/trips", jwt, body);

        return parseTripResponse(responseBody);
    }

    /**
     * Cancel an existing trip (passenger-initiated).
     *
     * @param context Application context
     * @param tripId  Backend trip id
     * @throws IOException   Network or HTTP error
     * @throws JSONException Unexpected response format
     */
    public static void cancelTrip(@NonNull Context context, @NonNull String tripId)
            throws IOException, JSONException {

        String jwt = getJwt(context);

        JSONObject body = new JSONObject();
        body.put("tripId", tripId);

        BackendApiService.executeAuthenticatedPost(
                context, "api/trips/" + tripId + "/cancel", jwt, body);
    }

    /**
     * Fetch the latest backend state for a trip.
     */
    @NonNull
    public static Trip fetchTrip(@NonNull Context context, @NonNull String tripId)
            throws IOException, JSONException {

        String jwt = getJwt(context);
        String responseBody = BackendApiService.executeAuthenticatedGet(
                context,
                "api/trips/" + tripId,
                jwt,
                new java.util.LinkedHashMap<>()
        );

        return parseTripResponse(responseBody);
    }

    /**
     * Fetch the newest active trip for the authenticated passenger or driver.
     */
    @Nullable
    public static Trip fetchActiveTrip(@NonNull Context context)
            throws IOException, JSONException {

        String jwt = getJwt(context);
        String responseBody = BackendApiService.executeAuthenticatedGet(
                context,
                "api/trips/active",
                jwt,
                new java.util.LinkedHashMap<>()
        );

        JSONObject root = new JSONObject(responseBody);
        JSONObject dataJson = root.optJSONObject("data");
        if (dataJson == null || dataJson.isNull("trip")) {
            return null;
        }

        JSONObject tripJson = dataJson.optJSONObject("trip");
        return tripJson == null ? null : parseTripJson(tripJson);
    }

    /**
     * Publish the driver's current position for the passenger's active-trip map.
     */
    @NonNull
    public static Trip updateDriverLocation(@NonNull Context context,
                                            @NonNull String tripId,
                                            double driverLat,
                                            double driverLng)
            throws IOException, JSONException {

        String jwt = getJwt(context);
        JSONObject body = new JSONObject();
        body.put("driverLat", driverLat);
        body.put("driverLng", driverLng);

        String responseBody = BackendApiService.executeAuthenticatedPatch(
                context,
                "api/trips/" + tripId + "/driver-location",
                jwt,
                body
        );

        return parseTripResponse(responseBody);
    }

    /**
     * Confirm pickup. Drivers should pass their current coordinates; passengers can pass nulls.
     */
    @NonNull
    public static Trip confirmPickup(@NonNull Context context,
                                     @NonNull String tripId,
                                     @Nullable Double driverLat,
                                     @Nullable Double driverLng)
            throws IOException, JSONException {

        String jwt = getJwt(context);
        JSONObject body = new JSONObject();
        if (driverLat != null && driverLng != null) {
            body.put("driverLat", driverLat);
            body.put("driverLng", driverLng);
        }

        String responseBody = BackendApiService.executeAuthenticatedPost(
                context,
                "api/trips/" + tripId + "/pickup-confirm",
                jwt,
                body
        );

        return parseTripResponse(responseBody);
    }

    /**
     * Complete an in-progress trip. Drivers must pass their current coordinates.
     */
    @NonNull
    public static Trip completeTrip(@NonNull Context context,
                                    @NonNull String tripId,
                                    double driverLat,
                                    double driverLng)
            throws IOException, JSONException {

        String jwt = getJwt(context);
        JSONObject body = new JSONObject();
        body.put("driverLat", driverLat);
        body.put("driverLng", driverLng);

        String responseBody = BackendApiService.executeAuthenticatedPost(
                context,
                "api/trips/" + tripId + "/complete",
                jwt,
                body
        );

        return parseTripResponse(responseBody);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    @NonNull
    private static String getJwt(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_JWT, "");
    }

    @NonNull
    private static Trip parseTripResponse(@NonNull String responseBody)
            throws IOException, JSONException {

        JSONObject root = new JSONObject(responseBody);

        // Backend may wrap data inside a "data" or "trip" key, or return it flat.
        JSONObject tripJson = root.optJSONObject("data");
        if (tripJson != null && tripJson.has("trip")) {
            tripJson = tripJson.optJSONObject("trip");
        } else if (tripJson == null) {
            // Try flat response
            tripJson = root;
        }

        if (tripJson == null || !tripJson.has("id")) {
            throw new IOException("Máy chủ không trả về thông tin chuyến hợp lệ");
        }

        return parseTripJson(tripJson);
    }

    @NonNull
    private static Trip parseTripJson(@NonNull JSONObject tripJson)
            throws IOException, JSONException {

        if (!tripJson.has("id")) {
            throw new IOException("Máy chủ không trả về thông tin chuyến hợp lệ");
        }

        String id          = String.valueOf(tripJson.opt("id"));
        double pickupLat   = tripJson.optDouble("pickupLat",         0);
        double pickupLng   = tripJson.optDouble("pickupLng",         0);
        String pickupAddr  = tripJson.optString("pickupAddress",     "");
        double destLat     = tripJson.optDouble("destinationLat",    0);
        double destLng     = tripJson.optDouble("destinationLng",    0);
        String destAddr    = tripJson.optString("destinationAddress","");
        double fare        = tripJson.optDouble("fare",              0);
        String status      = tripJson.optString("status",            "pending");
        double distance    = tripJson.optDouble("distance",          0);
        String tripType    = tripJson.optString("tripType",          "");

        Trip trip = new Trip(id, pickupLat, pickupLng, pickupAddr,
                        destLat, destLng, destAddr, fare, status, distance, tripType);
        applyPickupConfirmation(trip, tripJson);
        applyDriverLocation(trip, tripJson);
        applyDriverInfo(trip, tripJson);
        return trip;
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
}
