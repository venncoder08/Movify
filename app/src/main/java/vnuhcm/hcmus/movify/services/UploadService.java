package vnuhcm.hcmus.movify.services;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Standalone service for uploading avatar images.
 * Uploads the picked image to the backend route, which forwards it to Cloudinary.
 */
public class UploadService {
    private static final String TAG = "UploadService";
    private static final String AVATAR_UPLOAD_ENDPOINT = "/api/uploads/avatar";
    private static final String IMAGE_UPLOAD_ENDPOINT = "/api/uploads/image";

    private final Context context;
    private final ExecutorService executorService;

    public UploadService(Context context) {
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void uploadImage(@NonNull Uri imageUri, String jwtToken, @NonNull UploadCallback callback) {
        uploadMultipartImage(imageUri, jwtToken, AVATAR_UPLOAD_ENDPOINT, "avatar", "avatar.jpg", callback);
    }

    public void uploadDocumentImage(@NonNull Uri imageUri, String jwtToken, @NonNull UploadCallback callback) {
        uploadMultipartImage(imageUri, jwtToken, IMAGE_UPLOAD_ENDPOINT, "image", "driver-document.jpg", callback);
    }

    public void uploadChatImage(@NonNull Uri imageUri, String jwtToken, @NonNull UploadCallback callback) {
        uploadMultipartImage(imageUri, jwtToken, IMAGE_UPLOAD_ENDPOINT, "image", "chat-image.jpg", callback);
    }

    private void uploadMultipartImage(@NonNull Uri imageUri,
                                      String jwtToken,
                                      String endpoint,
                                      String fieldName,
                                      String fileName,
                                      @NonNull UploadCallback callback) {
        executorService.execute(() -> {
            HttpURLConnection connection = null;
            try {
                String boundary = "----MovifyBoundary" + UUID.randomUUID();
                String requestUrl = BackendApiService.resolveBackendBaseUrl(context) + endpoint;

                connection = (HttpURLConnection) new URL(requestUrl).openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setUseCaches(false);
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("Authorization", "Bearer " + jwtToken);
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                try (OutputStream outputStream = connection.getOutputStream();
                     BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
                     InputStream inputStream = context.getContentResolver().openInputStream(imageUri)) {

                    if (inputStream == null) {
                        throw new IOException("Không đọc được ảnh đã chọn");
                    }

                    String mimeType = context.getContentResolver().getType(imageUri);
                    if (mimeType == null || mimeType.isBlank()) {
                        mimeType = "image/jpeg";
                    }

                    writer.write("--" + boundary + "\r\n");
                    writer.write("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + fileName + "\"\r\n");
                    writer.write("Content-Type: " + mimeType + "\r\n\r\n");
                    writer.flush();

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    outputStream.flush();

                    writer.write("\r\n--" + boundary + "--\r\n");
                    writer.flush();
                }

                int responseCode = connection.getResponseCode();
                String responseBody = BackendApiService.readResponse(connection, responseCode);

                if (responseCode < HttpURLConnection.HTTP_OK || responseCode >= HttpURLConnection.HTTP_MULT_CHOICE) {
                    throw new IOException(BackendApiService.extractErrorMessage(
                            responseBody,
                            "Tải ảnh lên thất bại " + responseCode
                    ));
                }

                String imageUrl = extractUploadedUrl(responseBody);
                if (imageUrl.isEmpty()) {
                    throw new IOException("Máy chủ không trả về đường dẫn ảnh");
                }

                callback.onSuccess(imageUrl);
            } catch (Exception exception) {
            callback.onFailure(exception.getMessage() == null ? "Tải ảnh lên thất bại" : exception.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    public interface UploadCallback {
        void onSuccess(String imageUrl);
        void onFailure(String errorMessage);
    }

    @NonNull
    private String extractUploadedUrl(String responseBody) {
        try {
            org.json.JSONObject root = new org.json.JSONObject(responseBody);
            org.json.JSONObject data = root.optJSONObject("data");
            if (data != null) {
                String url = data.optString("url", "").trim();
                if (!url.isEmpty()) {
                    return url;
                }

                String secureUrl = data.optString("secure_url", "").trim();
                if (!secureUrl.isEmpty()) {
                    return secureUrl;
                }
            }
        } catch (Exception ignored) {
            // Fall through to empty string.
        }

        return "";
    }
}
