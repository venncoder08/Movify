package vnuhcm.hcmus.movify.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vnuhcm.hcmus.movify.R;
import vnuhcm.hcmus.movify.activities.ChatActivity;
import vnuhcm.hcmus.movify.adapters.ConversationAdapter;
import vnuhcm.hcmus.movify.models.ConversationSummary;
import vnuhcm.hcmus.movify.services.MessageService;
import vnuhcm.hcmus.movify.services.SocketManager;

public class MessageFragment extends Fragment implements SocketManager.SocketEventListener {

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<ConversationSummary> conversations = new ArrayList<>();

    private ExecutorService dataExecutor;
    private ConversationAdapter adapter;
    private ListView lvConversations;
    private TextView tvEmpty;

    public MessageFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_message, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        lvConversations = view.findViewById(R.id.lvConversations);
        tvEmpty = view.findViewById(R.id.tvConversationsEmpty);
        dataExecutor = Executors.newSingleThreadExecutor();
        adapter = new ConversationAdapter(requireContext(), conversations);
        lvConversations.setAdapter(adapter);
        lvConversations.setEmptyView(tvEmpty);
        lvConversations.setOnItemClickListener((parent, itemView, position, id) -> openConversation(conversations.get(position)));
    }

    @Override
    public void onStart() {
        super.onStart();
        SocketManager.getInstance(requireContext()).connect();
        SocketManager.getInstance(requireContext()).addListener(this);
        loadConversations();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (isAdded()) {
            SocketManager.getInstance(requireContext()).removeListener(this);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mainHandler.removeCallbacksAndMessages(null);
        if (dataExecutor != null) {
            dataExecutor.shutdownNow();
            dataExecutor = null;
        }
    }

    private void loadConversations() {
        if (dataExecutor == null || dataExecutor.isShutdown()) {
            return;
        }

        final android.content.Context appContext = requireContext().getApplicationContext();
        dataExecutor.execute(() -> {
            try {
                List<ConversationSummary> loadedItems = MessageService.fetchConversations(appContext);
                mainHandler.post(() -> {
                    conversations.clear();
                    conversations.addAll(loadedItems);
                    adapter.notifyDataSetChanged();
                });
            } catch (Exception exception) {
                mainHandler.post(() -> Toast.makeText(requireContext(), exception.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void openConversation(@NonNull ConversationSummary conversation) {
        Intent intent = new Intent(requireContext(), ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_ACCOUNT_ID, conversation.getParticipant().getId());
        intent.putExtra(ChatActivity.EXTRA_TITLE, conversation.getParticipant().getFullName());
        startActivity(intent);
    }

    @Override
    public void onSocketEvent(@NonNull String eventName, @Nullable JSONObject payload) {
        loadConversations();
    }
}
