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
            throw new IOException("Backend không trả về thông tin trip hợp lệ");
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

        return new Trip(id, pickupLat, pickupLng, pickupAddr,
                        destLat, destLng, destAddr, fare, status, distance);
    }
}
