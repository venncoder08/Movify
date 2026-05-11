package vnuhcm.hcmus.movify.activities;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vnuhcm.hcmus.movify.R;
import vnuhcm.hcmus.movify.adapters.ChatMessageAdapter;
import vnuhcm.hcmus.movify.models.ChatMessage;
import vnuhcm.hcmus.movify.models.ConversationDetail;
import vnuhcm.hcmus.movify.models.TripConversationDetail;
import vnuhcm.hcmus.movify.services.AuthApiService;
import vnuhcm.hcmus.movify.services.MessageService;
import vnuhcm.hcmus.movify.services.SocketManager;
import vnuhcm.hcmus.movify.services.UploadService;

public class ChatActivity extends AppCompatActivity implements SocketManager.SocketEventListener {

    public static final String EXTRA_ACCOUNT_ID = "extra_account_id";
    public static final String EXTRA_TRIP_ID = "extra_trip_id";
    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_HISTORY_MODE = "extra_history_mode";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<ChatMessage> messages = new ArrayList<>();

    private ExecutorService dataExecutor;
    private ChatMessageAdapter adapter;
    private UploadService uploadService;

    private int accountId;
    private int tripId;
    private boolean historyMode;
    private boolean canSend;

    private TextView tvChatTitle;
    private TextView tvChatStatus;
    private ListView lvMessages;
    private EditText edtMessage;
    private ImageView btnPickImage;
    private TextView btnSend;
    private View composerContainer;

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::handlePickedImage);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        accountId = getIntent().getIntExtra(EXTRA_ACCOUNT_ID, 0);
        tripId = getIntent().getIntExtra(EXTRA_TRIP_ID, 0);
        historyMode = getIntent().getBooleanExtra(EXTRA_HISTORY_MODE, false);

        dataExecutor = Executors.newSingleThreadExecutor();
        uploadService = new UploadService(this);

        tvChatTitle = findViewById(R.id.tvChatTitle);
        tvChatStatus = findViewById(R.id.tvChatStatus);
        lvMessages = findViewById(R.id.lvChatMessages);
        edtMessage = findViewById(R.id.edtChatMessage);
        btnPickImage = findViewById(R.id.btnPickChatImage);
        btnSend = findViewById(R.id.btnSendChat);
        composerContainer = findViewById(R.id.chatComposerContainer);

        adapter = new ChatMessageAdapter(this, messages);
        lvMessages.setAdapter(adapter);

        tvChatTitle.setText(getIntent().getStringExtra(EXTRA_TITLE));
        findViewById(R.id.btnBackChat).setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> sendTextMessage());
        btnPickImage.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        SocketManager.getInstance(this).connect();
        if (!historyMode) {
            SocketManager.getInstance(this).addListener(this);
        }

        loadConversation(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mainHandler.removeCallbacksAndMessages(null);
        SocketManager.getInstance(this).removeListener(this);
        if (dataExecutor != null) {
            dataExecutor.shutdownNow();
            dataExecutor = null;
        }
    }

    private void loadConversation(boolean initial) {
        if (dataExecutor == null || dataExecutor.isShutdown()) {
            return;
        }

        final android.content.Context appContext = getApplicationContext();
        dataExecutor.execute(() -> {
            try {
                if (historyMode) {
                    TripConversationDetail detail = MessageService.fetchTripMessages(appContext, tripId);
                    mainHandler.post(() -> bindTripHistory(detail, initial));
                    return;
                }

                ConversationDetail detail = MessageService.fetchConversation(appContext, accountId);
                mainHandler.post(() -> bindConversation(detail, initial));
                MessageService.markConversationAsRead(appContext, accountId);
            } catch (Exception exception) {
                mainHandler.post(() -> Toast.makeText(this, exception.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void bindConversation(@NonNull ConversationDetail detail, boolean initial) {
        if (detail.getParticipant() != null && !TextUtils.isEmpty(detail.getParticipant().getFullName())) {
            tvChatTitle.setText(detail.getParticipant().getFullName());
        }

        messages.clear();
        messages.addAll(detail.getMessages());
        adapter.notifyDataSetChanged();
        canSend = detail.canSend();
        composerContainer.setVisibility(View.VISIBLE);
        edtMessage.setEnabled(canSend);
        btnSend.setEnabled(canSend);
        btnPickImage.setEnabled(canSend);
        tvChatStatus.setText(canSend
                ? getString(R.string.message_status_active)
                : getString(R.string.message_status_read_only));
        scrollToBottom(initial);
    }

    private void bindTripHistory(@NonNull TripConversationDetail detail, boolean initial) {
        messages.clear();
        messages.addAll(detail.getMessages());
        adapter.notifyDataSetChanged();
        canSend = false;
        composerContainer.setVisibility(View.GONE);
        tvChatStatus.setText(getString(R.string.message_status_history, detail.getTripId()));
        scrollToBottom(initial);
    }

    private void sendTextMessage() {
        if (!canSend || accountId <= 0 || dataExecutor == null || dataExecutor.isShutdown()) {
            return;
        }

        String content = edtMessage.getText().toString().trim();
        if (content.isEmpty()) {
            return;
        }

        edtMessage.setText("");
        final android.content.Context appContext = getApplicationContext();
        dataExecutor.execute(() -> {
            try {
                MessageService.sendTextMessage(appContext, accountId, content);
                mainHandler.post(() -> loadConversation(false));
            } catch (Exception exception) {
                mainHandler.post(() -> {
                    edtMessage.setText(content);
                    Toast.makeText(this, exception.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void handlePickedImage(@Nullable Uri uri) {
        if (uri == null || !canSend || accountId <= 0 || dataExecutor == null || dataExecutor.isShutdown()) {
            return;
        }

        String jwtToken = new AuthApiService(this).getJwtToken();
        if (jwtToken == null || jwtToken.trim().isEmpty()) {
            Toast.makeText(this, R.string.missing_login_session, Toast.LENGTH_SHORT).show();
            return;
        }

        tvChatStatus.setText(R.string.message_uploading_image);
        uploadService.uploadChatImage(uri, jwtToken, new UploadService.UploadCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                if (dataExecutor == null || dataExecutor.isShutdown()) {
                    return;
                }

                dataExecutor.execute(() -> {
                    try {
                        MessageService.sendImageMessage(getApplicationContext(), accountId, imageUrl, "");
                        mainHandler.post(() -> {
                            tvChatStatus.setText(canSend
                                    ? getString(R.string.message_status_active)
                                    : getString(R.string.message_status_read_only));
                            loadConversation(false);
                        });
                    } catch (Exception exception) {
                        mainHandler.post(() -> Toast.makeText(ChatActivity.this, exception.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                mainHandler.post(() -> {
                    tvChatStatus.setText(canSend
                            ? getString(R.string.message_status_active)
                            : getString(R.string.message_status_read_only));
                    Toast.makeText(ChatActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void scrollToBottom(boolean initial) {
        if (messages.isEmpty()) {
            return;
        }

        lvMessages.post(() -> lvMessages.setSelection(adapter.getCount() - 1));
    }

    @Override
    public void onSocketEvent(@NonNull String eventName, @Nullable JSONObject payload) {
        if (historyMode || accountId <= 0) {
            return;
        }

        int payloadParticipantId = payload == null ? 0 : payload.optInt("participantId", 0);
        if (payloadParticipantId != accountId) {
            return;
        }

        loadConversation(false);
    }
}
