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

import vnuhcm.hcmus.movify.models.ChatMessage;
import vnuhcm.hcmus.movify.models.ChatParticipant;
import vnuhcm.hcmus.movify.models.ConversationDetail;
import vnuhcm.hcmus.movify.models.ConversationSummary;
import vnuhcm.hcmus.movify.models.TripConversationDetail;

public final class MessageService {

    private MessageService() {
    }

    @NonNull
    public static List<ConversationSummary> fetchConversations(@NonNull Context context)
            throws IOException, JSONException {
        String responseBody = BackendApiService.executeAuthenticatedGet(
                context,
                "api/messages",
                requireJwtToken(context),
                new LinkedHashMap<>()
        );

        JSONObject root = new JSONObject(responseBody);
        JSONObject data = root.optJSONObject("data");
        JSONArray items = data == null ? null : data.optJSONArray("items");
        List<ConversationSummary> conversations = new ArrayList<>();

        if (items == null) {
            return conversations;
        }

        for (int index = 0; index < items.length(); index++) {
            JSONObject item = items.optJSONObject(index);
            if (item == null) {
                continue;
            }

            ChatParticipant participant = parseParticipant(item.optJSONObject("participant"));
            if (participant == null) {
                continue;
            }

            JSONObject lastMessage = item.optJSONObject("lastMessage");
            JSONObject activeTrip = item.optJSONObject("activeTrip");
            conversations.add(new ConversationSummary(
                    participant,
                    buildPreview(lastMessage),
                    item.optString("updatedAt", ""),
                    item.optInt("unreadCount", 0),
                    activeTrip == null ? null : activeTrip.optInt("id", 0),
                    activeTrip == null ? "" : activeTrip.optString("status", ""),
                    item.isNull("lastTripId") ? null : item.optInt("lastTripId", 0),
                    item.optBoolean("canSend", false)
            ));
        }

        return conversations;
    }

    @NonNull
    public static ConversationDetail fetchConversation(@NonNull Context context, int accountId)
            throws IOException, JSONException {
        String responseBody = BackendApiService.executeAuthenticatedGet(
                context,
                "api/messages/" + accountId,
                requireJwtToken(context),
                new LinkedHashMap<>()
        );

        JSONObject root = new JSONObject(responseBody);
        JSONObject data = root.optJSONObject("data");
        if (data == null) {
            throw new IOException("Không tải được hội thoại");
        }

        JSONObject activeTrip = data.optJSONObject("activeTrip");
        return new ConversationDetail(
                parseParticipant(data.optJSONObject("participant")),
                parseMessages(data.optJSONArray("items")),
                data.optBoolean("canSend", false),
                activeTrip == null ? null : activeTrip.optInt("id", 0),
                activeTrip == null ? "" : activeTrip.optString("status", "")
        );
    }

    @NonNull
    public static TripConversationDetail fetchTripMessages(@NonNull Context context, int tripId)
            throws IOException, JSONException {
        String responseBody = BackendApiService.executeAuthenticatedGet(
                context,
                "api/trips/" + tripId + "/messages",
                requireJwtToken(context),
                new LinkedHashMap<>()
        );

        JSONObject root = new JSONObject(responseBody);
        JSONObject data = root.optJSONObject("data");
        if (data == null) {
            throw new IOException("Không tải được lịch sử chat");
        }

        JSONObject trip = data.optJSONObject("trip");
        return new TripConversationDetail(
                trip == null ? tripId : trip.optInt("id", tripId),
                trip == null ? "" : trip.optString("status", ""),
                data.isNull("participantId") ? null : data.optInt("participantId", 0),
                parseMessages(data.optJSONArray("items"))
        );
    }

    @NonNull
    public static ChatMessage sendTextMessage(@NonNull Context context, int accountId, @NonNull String content)
            throws IOException, JSONException {
        JSONObject body = new JSONObject();
        body.put("contentType", "Text");
        body.put("content", content.trim());

        return parseCreatedMessage(BackendApiService.executeAuthenticatedPost(
                context,
                "api/messages/" + accountId,
                requireJwtToken(context),
                body
        ));
    }

    @NonNull
    public static ChatMessage sendImageMessage(@NonNull Context context,
                                               int accountId,
                                               @NonNull String mediaUrl,
                                               @NonNull String content)
            throws IOException, JSONException {
        JSONObject body = new JSONObject();
        body.put("contentType", "Image");
        body.put("content", content.trim());
        body.put("mediaUrl", mediaUrl.trim());

        return parseCreatedMessage(BackendApiService.executeAuthenticatedPost(
                context,
                "api/messages/" + accountId,
                requireJwtToken(context),
                body
        ));
    }

    public static void markConversationAsRead(@NonNull Context context, int accountId)
            throws IOException {
        BackendApiService.executeAuthenticatedPatch(
                context,
                "api/messages/" + accountId + "/read",
                requireJwtToken(context),
                new JSONObject()
        );
    }

    @NonNull
    private static String requireJwtToken(@NonNull Context context) throws IOException {
        String jwtToken = new AuthApiService(context).getJwtToken();
        if (jwtToken == null || jwtToken.isBlank()) {
            throw new IOException("Không tìm thấy mã phiên đăng nhập");
        }
        return jwtToken;
    }

    @NonNull
    private static ChatMessage parseCreatedMessage(@NonNull String responseBody) throws JSONException, IOException {
        JSONObject root = new JSONObject(responseBody);
        JSONObject data = root.optJSONObject("data");
        if (data == null) {
            throw new IOException("Không gửi được tin nhắn");
        }

        JSONObject messageJson = data.optJSONObject("message");
        if (messageJson == null) {
            throw new IOException("Máy chủ không trả về tin nhắn hợp lệ");
        }

        return parseMessage(messageJson);
    }

    @NonNull
    private static List<ChatMessage> parseMessages(JSONArray items) {
        List<ChatMessage> messages = new ArrayList<>();
        if (items == null) {
            return messages;
        }

        for (int index = 0; index < items.length(); index++) {
            JSONObject message = items.optJSONObject(index);
            if (message == null) {
                continue;
            }
            messages.add(parseMessage(message));
        }

        return messages;
    }

    @NonNull
    private static ChatMessage parseMessage(@NonNull JSONObject item) {
        Integer tripId = item.isNull("tripId") ? null : item.optInt("tripId", 0);
        return new ChatMessage(
                item.optInt("id", 0),
                tripId,
                item.optString("content", ""),
                item.optString("contentType", "Text"),
                item.optString("mediaUrl", ""),
                item.optBoolean("isRead", false),
                item.optString("createdAt", ""),
                item.optBoolean("isMine", false)
        );
    }

    private static ChatParticipant parseParticipant(JSONObject item) {
        if (item == null) {
            return null;
        }

        return new ChatParticipant(
                item.optInt("id", 0),
                item.optString("fullName", ""),
                item.optString("avatar", ""),
                item.optString("phoneNumber", "")
        );
    }

    @NonNull
    private static String buildPreview(JSONObject lastMessage) {
        if (lastMessage == null) {
            return "";
        }

        String contentType = lastMessage.optString("contentType", "Text");
        if ("Image".equalsIgnoreCase(contentType)) {
            String caption = lastMessage.optString("content", "").trim();
            return caption.isEmpty() ? "[Ảnh]" : "[Ảnh] " + caption;
        }

        return lastMessage.optString("content", "");
    }
}
