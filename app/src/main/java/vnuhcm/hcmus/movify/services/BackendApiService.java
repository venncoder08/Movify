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
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

class BackendApiService {

    private static final String BACKEND_BASE_URL_META_DATA = "com.movify.BACKEND_BASE_URL";
    private static final String DEFAULT_BACKEND_BASE_URL = "http://10.0.2.2:3000";

    private BackendApiService() {
    }

    @NonNull
    static String executeGet(Context context, String path, Map<String, String> queryParams)
            throws IOException, JSONException {
        String requestUrl = buildRequestUrl(resolveBackendBaseUrl(context), path, queryParams);
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(requestUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            int responseCode = connection.getResponseCode();
            String responseBody = readResponse(connection, responseCode);
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException(extractErrorMessage(responseBody, "Backend loi HTTP " + responseCode));
            }

            return responseBody;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @NonNull
    private static String readResponse(HttpURLConnection connection, int responseCode) throws IOException {
        java.io.InputStream responseStream = responseCode >= HttpURLConnection.HTTP_BAD_REQUEST
                ? connection.getErrorStream()
                : connection.getInputStream();

        if (responseStream == null) {
            return "";
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(responseStream))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            return builder.toString();
        }
    }

    @NonNull
    private static String extractErrorMessage(String responseBody, String fallback) throws JSONException {
        if (responseBody == null || responseBody.isBlank()) {
            return fallback;
        }

        JSONObject root = new JSONObject(responseBody);
        String message = root.optString("message", "").trim();
        return message.isEmpty() ? fallback : message;
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
    private static String resolveBackendBaseUrl(Context context) throws IOException {
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
                throw new IOException("BACKEND_BASE_URL khong hop le");
            }

            return trimTrailingSlash(baseUrl);
        } catch (PackageManager.NameNotFoundException exception) {
            throw new IOException("Khong doc duoc BACKEND_BASE_URL", exception);
        }
    }

    @NonNull
    private static String trimTrailingSlash(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }
}
