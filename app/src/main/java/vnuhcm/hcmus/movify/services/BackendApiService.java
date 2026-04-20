package vnuhcm.hcmus.movify.services;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import vnuhcm.hcmus.movify.R;

class BackendApiService {

    static final String BACKEND_BASE_URL_META_DATA = "com.movify.BACKEND_BASE_URL";
    static final String DEFAULT_BACKEND_BASE_URL = "http://10.0.2.2:3000";

    private BackendApiService() {
    }

    @NonNull
    static String executeGet(Context context, String path, Map<String, String> queryParams)
            throws IOException, JSONException {
        return executeRequest(context, "GET", path, null, null, queryParams);
    }

    @NonNull
    static String executeAuthenticatedGet(Context context, String path, String jwtToken, Map<String, String> queryParams)
            throws IOException {
        return executeRequest(context, "GET", path, jwtToken, null, queryParams);
    }

    @NonNull
    static String executeAuthenticatedPost(Context context, String path, String jwtToken, JSONObject requestBody)
            throws IOException {
        return executeRequest(context, "POST", path, jwtToken, requestBody, Collections.emptyMap());
    }

    @NonNull
    static String executeAuthenticatedPatch(Context context, String path, String jwtToken, JSONObject requestBody)
            throws IOException {
        return executeRequest(context, "PATCH", path, jwtToken, requestBody, Collections.emptyMap());
    }

    @NonNull
    static String readResponse(HttpURLConnection connection, int responseCode) throws IOException {
        java.io.InputStream responseStream = responseCode >= HttpURLConnection.HTTP_BAD_REQUEST
                ? connection.getErrorStream()
                : connection.getInputStream();

        if (responseStream == null) {
            return "";
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(responseStream, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            return builder.toString();
        }
    }

    @NonNull
    static String extractErrorMessage(String responseBody, String fallback) {
        if (responseBody == null || responseBody.isBlank()) {
            return fallback;
        }

        try {
            JSONObject root = new JSONObject(responseBody);
            String message = root.optString("message", "").trim();
            return message.isEmpty() ? fallback : message;
        } catch (JSONException exception) {
            return fallback;
        }
    }

    @NonNull
    private static String buildRequestUrl(String baseUrl, String path, Map<String, String> queryParams) {
        Uri.Builder builder = Uri.parse(trimTrailingSlash(baseUrl)).buildUpon();
        builder.appendEncodedPath(path);

        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            if (entry.getValue() != null) {
                builder.appendQueryParameter(entry.getKey(), entry.getValue());
            }
        }

        return builder.build().toString();
    }

    @NonNull
    private static String executeRequest(Context context,
                                         String method,
                                         String path,
                                         String jwtToken,
                                         JSONObject requestBody,
                                         Map<String, String> queryParams) throws IOException {
        String requestUrl = buildRequestUrl(
                resolveBackendBaseUrl(context),
                path,
                queryParams == null ? Collections.emptyMap() : queryParams
        );
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(requestUrl).openConnection();
            connection.setRequestMethod(method);
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            if (jwtToken != null && !jwtToken.isBlank()) {
                connection.setRequestProperty("Authorization", "Bearer " + jwtToken);
            }

            if (requestBody != null) {
                connection.setRequestProperty("Content-Type", "application/json");
                byte[] requestBytes = requestBody.toString().getBytes(StandardCharsets.UTF_8);
                connection.setDoOutput(true);
                try (OutputStream outputStream = connection.getOutputStream()) {
                    outputStream.write(requestBytes);
                    outputStream.flush();
                }
            }

            int responseCode = connection.getResponseCode();
            String responseBody = readResponse(connection, responseCode);
            if (responseCode < HttpURLConnection.HTTP_OK || responseCode >= HttpURLConnection.HTTP_MULT_CHOICE) {
                throw new IOException(extractErrorMessage(responseBody, context.getString(R.string.backend_error) + " " + responseCode));
            }

            return responseBody;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @NonNull
    static String resolveBackendBaseUrl(Context context) throws IOException {
        try {
            ApplicationInfo applicationInfo = context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);

            String configuredBaseUrl = null;
            if (applicationInfo.metaData != null) {
                configuredBaseUrl = applicationInfo.metaData.getString(BACKEND_BASE_URL_META_DATA);
            }

            String baseUrl = (configuredBaseUrl == null || configuredBaseUrl.isBlank())
                    ? DEFAULT_BACKEND_BASE_URL
                    : configuredBaseUrl.trim();

            if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
                throw new IOException("BACKEND_BASE_URL không hợp lệ");
            }

            return trimTrailingSlash(baseUrl);
        } catch (PackageManager.NameNotFoundException exception) {
            throw new IOException("Không đọc được BACKEND_BASE_URL", exception);
        }
    }

    @NonNull
    static String trimTrailingSlash(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }
}
