package vnuhcm.hcmus.movify.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import vnuhcm.hcmus.movify.R;
import vnuhcm.hcmus.movify.models.AccountProfile;

/**
 * Service for authentication API calls
 * - Google Sign-In token exchange
 * - Email/Password login
 * - Token management
 */
public class AuthApiService {
    private static final String TAG = "AuthApiService";
    private static final String PREFS_NAME = "auth_prefs";
    private static final String KEY_JWT_TOKEN = "jwt_token";
    private static final String KEY_USER_EMAIL = "user_email";

    private final Context context;
    private final SharedPreferences prefs;

    public AuthApiService(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Authenticate with Google Sign-In token
     * Exchange Google idToken for JWT token from backend
     *
     * @param idToken Google ID token from Firebase
     * @return JWT token from backend
     */
    public String authenticateWithGoogle(String idToken) throws IOException, JSONException {
        String endpoint = "/api/auth/google";
        JSONObject requestBody = new JSONObject();
        requestBody.put("idToken", idToken);

        String response = executePost(endpoint, requestBody);
        JSONObject responseObj = new JSONObject(response);

        String jwtToken = extractToken(responseObj);
        String email = extractEmail(responseObj, "");

        if (!jwtToken.isEmpty()) {
            saveJwtToken(jwtToken);
            saveUserEmail(email);
            Log.d(TAG, "Google authentication successful, JWT saved");
        }

        return jwtToken;
    }

    /**
     * Email/Password login
     *
     * @param email User email
     * @param password User password
     * @return JWT token
     */
    public String loginWithEmail(String email, String password) throws IOException, JSONException {
        String endpoint = "/api/auth/login";
        JSONObject requestBody = new JSONObject();
        requestBody.put("email", email);
        requestBody.put("password", password);

        String response = executePost(endpoint, requestBody);
        JSONObject responseObj = new JSONObject(response);

        String jwtToken = extractToken(responseObj);

        if (!jwtToken.isEmpty()) {
            saveJwtToken(jwtToken);
            saveUserEmail(extractEmail(responseObj, email));
            Log.d(TAG, "Email login successful, JWT saved");
        }

        return jwtToken;
    }

    /**
     * Request OTP for password reset
     *
     * @param email User email
     * @return OTP sent message
     */
    public String requestOTP(String email) throws IOException, JSONException {
        return requestOTP(email, "reset-password");
    }

    public String requestOTP(String email, String purpose) throws IOException, JSONException {
        String endpoint = "/api/otp/request";
        JSONObject requestBody = new JSONObject();
        requestBody.put("email", email);
        requestBody.put("purpose", purpose);

        String response = executePost(endpoint, requestBody);
        JSONObject responseObj = new JSONObject(response);

        return responseObj.optString("message", "OTP sent to email");
    }

    /**
     * Update user profile
     *
     * @param fullName User's full name
     * @param phoneNumber User's phone number
     * @param avatar URL of user's avatar image
     * @param gender User's gender
     * @param dateOfBirth in format "YYYY-MM-DD"
     * @return Success message
     */
    public String updateProfile(String fullName,
                                String phoneNumber,
                                String avatar,
                                String gender,
                                String dateOfBirth) throws IOException, JSONException {
        String endpoint = "/api/auth/me";
        JSONObject requestBody = new JSONObject();

        putIfPresent(requestBody, "fullName", fullName);
        putIfPresent(requestBody, "phoneNumber", phoneNumber);
        putIfPresent(requestBody, "avatar", avatar);
        putIfPresent(requestBody, "gender", gender);
        putIfPresent(requestBody, "dateOfBirth", dateOfBirth);

        String response = executePatch(endpoint, requestBody, getJwtToken());
        JSONObject responseObj = new JSONObject(response);

        String newToken = extractToken(responseObj);
        if (!newToken.isEmpty()) {
            saveJwtToken(newToken);
            Log.d(TAG, "Profile updated, new JWT saved");
        }

        return responseObj.optString("message", "Profile updated successfully");
    }

    private void putIfPresent(JSONObject requestBody, String key, String value) throws JSONException {
        if (value != null && !value.trim().isEmpty()) {
            requestBody.put(key, value.trim());
        }
    }

    /**
     * Verify OTP and reset password
     *
     * @param email User email
     * @param otp 6-digit OTP
     * @param newPassword New password
     * @return Success message
     */
    public String resetPasswordWithOTP(String email, String otp, String newPassword)
            throws IOException, JSONException {
        String endpoint = "/api/auth/reset-password";
        JSONObject requestBody = new JSONObject();
        requestBody.put("email", email);
        requestBody.put("otp", otp);
        requestBody.put("newPassword", newPassword);
        requestBody.put("confirmPassword", newPassword);

        String response = executePost(endpoint, requestBody);
        JSONObject responseObj = new JSONObject(response);

        return responseObj.optString("message", "Password reset successful");
    }

    public String registerWithOTP(String fullName,
                                  String phoneNumber,
                                  String email,
                                  String otp,
                                  String password) throws IOException, JSONException {
        String endpoint = "/api/otp/register";
        JSONObject requestBody = new JSONObject();
        requestBody.put("fullName", fullName);
        requestBody.put("phoneNumber", phoneNumber);
        requestBody.put("email", email);
        requestBody.put("otp", otp);
        requestBody.put("password", password);
        requestBody.put("role", "User");

        String response = executePost(endpoint, requestBody);
        JSONObject responseObj = new JSONObject(response);

        String jwtToken = extractToken(responseObj);
        if (!jwtToken.isEmpty()) {
            saveJwtToken(jwtToken);
            saveUserEmail(extractEmail(responseObj, email));
            Log.d(TAG, "Register with OTP successful, JWT saved");
        }

        return jwtToken;
    }

    /**
     * Logout - invalidate JWT token
     *
     * @return Success message
     */
    public String logout() throws IOException, JSONException {
        String endpoint = "/api/auth/logout";
        String jwtToken = getJwtToken();

        JSONObject requestBody = new JSONObject();
        String response = executePost(endpoint, requestBody, jwtToken);

        clearJwtToken();
        clearUserEmail();
        Log.d(TAG, "Logout successful");

        JSONObject responseObj = new JSONObject(response);
        return responseObj.optString("message", "Logged out");
    }

    /**
     * Get user profile with JWT token
     *
     * @return User profile JSON
     */
    public String getProfile() throws IOException, JSONException {
        String endpoint = "/api/auth/me";
        String jwtToken = getJwtToken();

        if (jwtToken == null || jwtToken.isEmpty()) {
            throw new IOException("No JWT token available");
        }

        return executeGet(endpoint, jwtToken);
    }

    public AccountProfile fetchCurrentUserProfile() throws IOException, JSONException {
        JSONObject responseObj = new JSONObject(getProfile());
        JSONObject data = responseObj.optJSONObject("data");

        if (data == null) {
            throw new IOException(context.getString(R.string.backend_no_account_info));
        }

        return new AccountProfile(
                data.optInt("id", 0),
                extractText(data, "fullName"),
                extractText(data, "phoneNumber"),
                extractText(data, "email"),
                extractText(data, "role"),
                extractText(data, "gender"),
                extractText(data, "dateOfBirth"),
                extractText(data, "avatar"),
                extractText(data, "idCard"),
                extractText(data, "driverLicense"),
                extractText(data, "vehicleRegistration"),
                extractText(data, "driverStatus")
        );
    }

    /**
     * Execute POST request with JWT authentication
     *
     * @param endpoint API endpoint path
     * @param requestBody JSON request body
     * @return Response body
     */
    private String executePost(String endpoint, JSONObject requestBody) throws IOException {
        return executePost(endpoint, requestBody, null);
    }

    /**
     * Execute POST request with optional JWT authentication
     *
     * @param endpoint API endpoint path
     * @param requestBody JSON request body
     * @param jwtToken JWT token (optional)
     * @return Response body
     */
    private String executePost(String endpoint, JSONObject requestBody, String jwtToken)
            throws IOException {
        URL url = new URL(BackendApiService.resolveBackendBaseUrl(context) + endpoint);
        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");

            if (jwtToken != null && !jwtToken.isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + jwtToken);
            }

            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            // Send request body
            byte[] jsonBytes = requestBody.toString().getBytes(StandardCharsets.UTF_8);
            connection.setDoOutput(true);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonBytes);
                os.flush();
            }

            int responseCode = connection.getResponseCode();
            String responseBody = readResponse(connection, responseCode);

            if (responseCode < 200 || responseCode >= 300) {
                throw new IOException(BackendApiService.extractErrorMessage(
                        responseBody,
                        "HTTP error " + responseCode
                ));
            }

            return responseBody;

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Execute PATCH request with optional JWT authentication
     *
     * @param endpoint API endpoint path
     * @param requestBody JSON request body
     * @param jwtToken JWT token (optional)
     * @return Response body
     */
    private String executePatch(String endpoint, JSONObject requestBody, String jwtToken)
            throws IOException {
        URL url = new URL(BackendApiService.resolveBackendBaseUrl(context) + endpoint);
        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("PATCH");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");

            if (jwtToken != null && !jwtToken.isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + jwtToken);
            }

            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            byte[] jsonBytes = requestBody.toString().getBytes(StandardCharsets.UTF_8);
            connection.setDoOutput(true);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonBytes);
                os.flush();
            }

            int responseCode = connection.getResponseCode();
            String responseBody = readResponse(connection, responseCode);

            if (responseCode < 200 || responseCode >= 300) {
                throw new IOException(BackendApiService.extractErrorMessage(
                        responseBody,
                        "HTTP error " + responseCode
                ));
            }

            return responseBody;

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Execute GET request with JWT authentication
     *
     * @param endpoint API endpoint path
     * @param jwtToken JWT token
     * @return Response body
     */
    private String executeGet(String endpoint, String jwtToken) throws IOException {
        URL url = new URL(BackendApiService.resolveBackendBaseUrl(context) + endpoint);
        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + jwtToken);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            int responseCode = connection.getResponseCode();
            String responseBody = readResponse(connection, responseCode);

            if (responseCode < 200 || responseCode >= 300) {
                throw new IOException(BackendApiService.extractErrorMessage(
                        responseBody,
                        "HTTP error " + responseCode
                ));
            }

            return responseBody;

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Read response from connection
     */
    private String readResponse(HttpURLConnection connection, int responseCode) throws IOException {
        return BackendApiService.readResponse(connection, responseCode);
    }

    private String extractToken(JSONObject responseObj) {
        String jwtToken = responseObj.optString("token", "");
        if (!jwtToken.isEmpty()) {
            return jwtToken;
        }

        JSONObject data = responseObj.optJSONObject("data");
        if (data == null) {
            return "";
        }

        return data.optString("token", "");
    }

    private String extractEmail(JSONObject responseObj, String fallbackEmail) {
        String email = extractText(responseObj, "email");
        if (!email.isEmpty()) {
            return email;
        }

        JSONObject user = responseObj.optJSONObject("user");
        if (user != null) {
            email = extractText(user, "email");
            if (!email.isEmpty()) {
                return email;
            }
        }

        JSONObject data = responseObj.optJSONObject("data");
        if (data != null) {
            JSONObject dataUser = data.optJSONObject("user");
            if (dataUser != null) {
                email = extractText(dataUser, "email");
                if (!email.isEmpty()) {
                    return email;
                }
            }
        }

        return fallbackEmail;
    }

    private String extractText(JSONObject source, String key) {
        String value = source.optString(key, "").trim();
        return "null".equalsIgnoreCase(value) ? "" : value;
    }

    /**
     * JWT Token management
     */
    public void saveJwtToken(String token) {
        prefs.edit().putString(KEY_JWT_TOKEN, token).apply();
    }

    public String getJwtToken() {
        return prefs.getString(KEY_JWT_TOKEN, null);
    }

    public void clearJwtToken() {
        prefs.edit().remove(KEY_JWT_TOKEN).apply();
    }

    /**
     * User email management
     */
    public void saveUserEmail(String email) {
        prefs.edit().putString(KEY_USER_EMAIL, email).apply();
    }

    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, null);
    }

    public void clearUserEmail() {
        prefs.edit().remove(KEY_USER_EMAIL).apply();
    }

    public void clearSession() {
        prefs.edit().clear().apply();
    }

    /**
     * Check if user is logged in
     */
    public boolean isLoggedIn() {
        return getJwtToken() != null && !getJwtToken().isEmpty();
    }
}
