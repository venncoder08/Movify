package vnuhcm.hcmus.movify.services;

import android.content.Context;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.LinkedHashMap;

import vnuhcm.hcmus.movify.R;
import vnuhcm.hcmus.movify.models.DriverRequestStatus;

public final class DriverRequestService {

    private DriverRequestService() {
    }

    @NonNull
    public static DriverRequestStatus fetchMyDriverRequest(Context context) throws IOException, JSONException {
        String jwtToken = requireJwtToken(context);
        String responseBody = BackendApiService.executeAuthenticatedGet(
                context,
                "api/driver-requests/me",
                jwtToken,
                new LinkedHashMap<>()
        );

        JSONObject root = new JSONObject(responseBody);
        JSONObject data = root.optJSONObject("data");
        if (data == null) {
            return new DriverRequestStatus("", "", "", "", "", "", "");
        }

        JSONObject profileDocuments = data.optJSONObject("profileDocuments");
        JSONObject latestRequest = data.optJSONObject("latestRequest");

        return new DriverRequestStatus(
                data.optString("accountRole", ""),
                data.optString("driverStatus", ""),
                firstNonEmpty(
                        latestRequest == null ? "" : latestRequest.optString("idCard", ""),
                        profileDocuments == null ? "" : profileDocuments.optString("idCard", "")
                ),
                firstNonEmpty(
                        latestRequest == null ? "" : latestRequest.optString("driverLicense", ""),
                        profileDocuments == null ? "" : profileDocuments.optString("driverLicense", "")
                ),
                firstNonEmpty(
                        latestRequest == null ? "" : latestRequest.optString("vehicleRegistration", ""),
                        profileDocuments == null ? "" : profileDocuments.optString("vehicleRegistration", "")
                ),
                latestRequest == null ? "" : latestRequest.optString("status", ""),
                latestRequest == null ? "" : latestRequest.optString("createdAt", "")
        );
    }

    @NonNull
    public static String submitDriverRequest(Context context,
                                             String idCard,
                                             String driverLicense,
                                             String vehicleRegistration) throws IOException, JSONException {
        String jwtToken = requireJwtToken(context);
        JSONObject requestBody = new JSONObject();
        requestBody.put("idCard", idCard);
        requestBody.put("driverLicense", driverLicense);
        requestBody.put("vehicleRegistration", vehicleRegistration);

        String responseBody = BackendApiService.executeAuthenticatedPost(
                context,
                "api/driver-requests",
                jwtToken,
                requestBody
        );

        JSONObject root = new JSONObject(responseBody);
        return root.optString("message", context.getString(R.string.submit_request_success));
    }

    @NonNull
    private static String requireJwtToken(Context context) throws IOException {
        String jwtToken = new AuthApiService(context).getJwtToken();
        if (jwtToken == null || jwtToken.isBlank()) {
            throw new IOException("No JWT token available");
        }
        return jwtToken;
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
}
