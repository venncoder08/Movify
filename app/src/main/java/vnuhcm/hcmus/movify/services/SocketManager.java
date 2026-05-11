package vnuhcm.hcmus.movify.services;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import vnuhcm.hcmus.movify.MovifyApplication;
import vnuhcm.hcmus.movify.utils.SystemNotificationHelper;

public final class SocketManager {

    public interface SocketEventListener {
        void onSocketEvent(@NonNull String eventName, @Nullable JSONObject payload);
    }

    private static final String EVENT_MESSAGE_NEW = "message:new";
    private static final String EVENT_CONVERSATION_READ = "conversation:read";
    private static final String EVENT_CONVERSATION_ELIGIBILITY = "conversation:eligibility";

    private static SocketManager instance;

    private final Context appContext;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Set<SocketEventListener> listeners = new CopyOnWriteArraySet<>();

    private Socket socket;
    private String activeToken;

    private final Emitter.Listener messageListener = args -> dispatchEvent(EVENT_MESSAGE_NEW, args);
    private final Emitter.Listener readListener = args -> dispatchEvent(EVENT_CONVERSATION_READ, args);
    private final Emitter.Listener eligibilityListener = args -> dispatchEvent(EVENT_CONVERSATION_ELIGIBILITY, args);

    private SocketManager(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
    }

    public static synchronized SocketManager getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new SocketManager(context);
        }
        return instance;
    }

    public synchronized void connect() {
        String token = new AuthApiService(appContext).getJwtToken();
        if (token == null || token.trim().isEmpty()) {
            disconnect();
            return;
        }

        if (socket != null && socket.connected() && token.equals(activeToken)) {
            return;
        }

        disconnect();

        try {
            IO.Options options = new IO.Options();
            options.reconnection = true;
            options.forceNew = false;
            options.auth = Collections.singletonMap("token", token);
            options.extraHeaders = Collections.singletonMap(
                    "Authorization",
                    Arrays.asList("Bearer " + token)
            );

            socket = IO.socket(BackendApiService.resolveBackendBaseUrl(appContext), options);
            activeToken = token;
            socket.on(EVENT_MESSAGE_NEW, messageListener);
            socket.on(EVENT_CONVERSATION_READ, readListener);
            socket.on(EVENT_CONVERSATION_ELIGIBILITY, eligibilityListener);
            socket.connect();
        } catch (URISyntaxException | java.io.IOException ignored) {
            socket = null;
            activeToken = null;
        }
    }

    public synchronized void disconnect() {
        if (socket != null) {
            socket.off(EVENT_MESSAGE_NEW, messageListener);
            socket.off(EVENT_CONVERSATION_READ, readListener);
            socket.off(EVENT_CONVERSATION_ELIGIBILITY, eligibilityListener);
            socket.disconnect();
            socket.close();
            socket = null;
        }
        activeToken = null;
    }

    public void addListener(@NonNull SocketEventListener listener) {
        listeners.add(listener);
    }

    public void removeListener(@NonNull SocketEventListener listener) {
        listeners.remove(listener);
    }

    private void dispatchEvent(@NonNull String eventName, @Nullable Object[] args) {
        JSONObject payload = null;
        if (args != null && args.length > 0 && args[0] instanceof JSONObject) {
            payload = (JSONObject) args[0];
        }

        JSONObject finalPayload = payload;
        mainHandler.post(() -> {
            maybeNotifyBackgroundMessage(eventName, finalPayload);
            for (SocketEventListener listener : listeners) {
                listener.onSocketEvent(eventName, finalPayload);
            }
        });
    }

    private void maybeNotifyBackgroundMessage(@NonNull String eventName, @Nullable JSONObject payload) {
        if (!EVENT_MESSAGE_NEW.equals(eventName) || payload == null || MovifyApplication.isAppInForeground()) {
            return;
        }

        JSONObject message = payload.optJSONObject("message");
        if (message == null || message.optBoolean("isMine", false)) {
            return;
        }

        int participantId = payload.optInt("participantId", 0);
        if (participantId <= 0) {
            return;
        }

        JSONObject sender = message.optJSONObject("sender");
        String participantName = sender == null ? "" : sender.optString("fullName", "");
        String content = message.optString("content", "").trim();
        String contentType = message.optString("contentType", "Text");
        if (content.isEmpty()) {
            content = "Image".equalsIgnoreCase(contentType) ? "[Image]" : "New message";
        }

        SystemNotificationHelper.notifyChatMessage(
                appContext,
                participantId,
                participantName,
                content,
                message.optInt("id", 0),
                payload.optInt("notificationId", 0)
        );
    }
}
