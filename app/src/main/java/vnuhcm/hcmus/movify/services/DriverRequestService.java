package vnuhcm.hcmus.movify.services;

import android.content.Context;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import vnuhcm.hcmus.movify.R;
import vnuhcm.hcmus.movify.models.DriverRequest;
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
            return new DriverRequestStatus("", "", "", "", "", "", "", "");
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
                firstNonEmpty(
                        latestRequest == null ? "" : latestRequest.optString("vehicleType", ""),
                        profileDocuments == null ? "" : profileDocuments.optString("vehicleType", "")
                ),
                latestRequest == null ? "" : latestRequest.optString("status", ""),
                latestRequest == null ? "" : latestRequest.optString("createdAt", "")
        );
    }

    @NonNull
    public static String submitDriverRequest(Context context,
                                             String idCard,
                                             String driverLicense,
                                             String vehicleRegistration,
                                             String vehicleType) throws IOException, JSONException {
        String jwtToken = requireJwtToken(context);
        JSONObject requestBody = new JSONObject();
        requestBody.put("idCard", idCard);
        requestBody.put("driverLicense", driverLicense);
        requestBody.put("vehicleRegistration", vehicleRegistration);
        requestBody.put("vehicleType", vehicleType);

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
    public static List<DriverRequest> fetchAdminDriverRequests(Context context, String status)
            throws IOException, JSONException {
        String jwtToken = requireJwtToken(context);
        Map<String, String> queryParams = new LinkedHashMap<>();
        if (status != null && !status.trim().isEmpty()) {
            queryParams.put("status", status);
        }

        String responseBody = BackendApiService.executeAuthenticatedGet(
                context,
                "api/driver-requests/admin",
                jwtToken,
                queryParams
        );

        JSONObject root = new JSONObject(responseBody);
        JSONArray data = root.optJSONArray("data");
        List<DriverRequest> requests = new ArrayList<>();

        if (data == null) {
            return requests;
        }

        for (int index = 0; index < data.length(); index++) {
            JSONObject requestJson = data.optJSONObject(index);
            if (requestJson != null) {
                requests.add(parseDriverRequest(requestJson));
            }
        }

        return requests;
    }

    @NonNull
    public static String updateDriverRequestStatus(Context context, int requestId, String status)
            throws IOException, JSONException {
        String jwtToken = requireJwtToken(context);
        JSONObject requestBody = new JSONObject();
        requestBody.put("status", status);

        String responseBody = BackendApiService.executeAuthenticatedPatch(
                context,
                "api/driver-requests/" + requestId + "/status",
                jwtToken,
                requestBody
        );

        JSONObject root = new JSONObject(responseBody);
        return root.optString("message", "");
    }

    @NonNull
    private static String requireJwtToken(Context context) throws IOException {
        String jwtToken = new AuthApiService(context).getJwtToken();
        if (jwtToken == null || jwtToken.isBlank()) {
            throw new IOException("Không tìm thấy mã phiên đăng nhập");
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

    @NonNull
    private static DriverRequest parseDriverRequest(JSONObject requestJson) {
        JSONObject driverJson = requestJson.optJSONObject("driver");

        return new DriverRequest(
                requestJson.optInt("id", 0),
                requestJson.optString("idCard", ""),
                requestJson.optString("driverLicense", ""),
                requestJson.optString("vehicleRegistration", ""),
                requestJson.optString("vehicleType", ""),
                requestJson.optString("status", ""),
                requestJson.optString("createdAt", ""),
                requestJson.optString("updatedAt", ""),
                driverJson == null ? "" : driverJson.optString("fullName", ""),
                driverJson == null ? "" : driverJson.optString("email", ""),
                driverJson == null ? "" : driverJson.optString("phoneNumber", ""),
                driverJson == null ? "" : driverJson.optString("avatar", "")
        );
    }
}
