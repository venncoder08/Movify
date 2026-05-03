package vnuhcm.hcmus.movify.fragments;

import android.content.Context;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vnuhcm.hcmus.movify.R;
import vnuhcm.hcmus.movify.adapters.NotificationAdapter;
import vnuhcm.hcmus.movify.models.Notification;
import vnuhcm.hcmus.movify.models.NotificationFeed;
import vnuhcm.hcmus.movify.services.NotificationService;

public class NotificationFragment extends Fragment {

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private ListView lvNotifications;
    private TextView tvNewBadge;
    private TextView tvMarkAllRead;
    private TextView tvEmptyState;
    private NotificationAdapter adapter;
    private final List<Notification> notificationList = new ArrayList<>();
    private ExecutorService notificationExecutor;

    public NotificationFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notification, container, false);

        lvNotifications = view.findViewById(R.id.lvNotifications);
        tvNewBadge = view.findViewById(R.id.tvNewBadge);
        tvMarkAllRead = view.findViewById(R.id.tvMarkAllRead);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);

        notificationExecutor = Executors.newSingleThreadExecutor();
        adapter = new NotificationAdapter(requireContext(), notificationList);
        lvNotifications.setAdapter(adapter);
        lvNotifications.setEmptyView(tvEmptyState);

        renderNotificationLoading();
        bindActions();
        loadNotifications();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mainHandler.removeCallbacksAndMessages(null);
        if (notificationExecutor != null) {
            notificationExecutor.shutdownNow();
            notificationExecutor = null;
        }
    }

    private void bindActions() {
        lvNotifications.setOnItemClickListener((parent, v, position, id) -> {
            Notification item = notificationList.get(position);
            if (item.isUnread()) {
                markNotificationAsRead(item);
            }
        });

        tvMarkAllRead.setOnClickListener(v -> markAllNotificationsAsRead());
    }

    private void loadNotifications() {
        if (notificationExecutor == null || notificationExecutor.isShutdown()) {
            return;
        }

        Context appContext = requireContext().getApplicationContext();
        notificationExecutor.execute(() -> {
            try {
                NotificationFeed feed = NotificationService.fetchNotifications(appContext);
                mainHandler.post(() -> bindNotifications(feed));
            } catch (Exception exception) {
                mainHandler.post(() -> renderNotificationError(exception));
            }
        });
    }

    private void bindNotifications(NotificationFeed feed) {
        if (!isAdded()) {
            return;
        }

        tvEmptyState.setText("Chưa có thông báo nào");
        notificationList.clear();
        notificationList.addAll(feed.getNotifications());
        adapter.notifyDataSetChanged();
        updateUnreadCount();
    }

    private void updateUnreadCount() {
        int count = 0;
        for (Notification item : notificationList) {
            if (item.isUnread()) {
                count++;
            }
        }

        tvNewBadge.setText(count + " mới");
        tvMarkAllRead.setEnabled(count > 0);
        tvMarkAllRead.setAlpha(count > 0 ? 1f : 0.5f);
    }

    private void renderNotificationLoading() {
        tvNewBadge.setText(R.string.loading_dots);
        tvMarkAllRead.setEnabled(false);
        tvMarkAllRead.setAlpha(0.5f);
        tvEmptyState.setText(R.string.loading_dots);
    }

    private void markNotificationAsRead(Notification item) {
        if (notificationExecutor == null || notificationExecutor.isShutdown()) {
            return;
        }

        Context appContext = requireContext().getApplicationContext();
        notificationExecutor.execute(() -> {
            try {
                NotificationService.markNotificationAsRead(appContext, item.getId());
                mainHandler.post(() -> {
                    item.setUnread(false);
                    adapter.notifyDataSetChanged();
                    updateUnreadCount();
                });
            } catch (Exception exception) {
                mainHandler.post(() -> showError(exception, "Không cập nhật được thông báo"));
            }
        });
    }

    private void markAllNotificationsAsRead() {
        if (notificationExecutor == null || notificationExecutor.isShutdown()) {
            return;
        }

        Context appContext = requireContext().getApplicationContext();
        notificationExecutor.execute(() -> {
            try {
                NotificationService.markAllNotificationsAsRead(appContext);
                mainHandler.post(() -> {
                    for (Notification notification : notificationList) {
                        notification.setUnread(false);
                    }
                    adapter.notifyDataSetChanged();
                    updateUnreadCount();
                });
            } catch (Exception exception) {
                mainHandler.post(() -> showError(exception, "Không đánh dấu đã đọc được"));
            }
        });
    }

    private void renderNotificationError(Exception exception) {
        if (!isAdded()) {
            return;
        }

        tvEmptyState.setText("Chưa có thông báo nào");
        notificationList.clear();
        adapter.notifyDataSetChanged();
        updateUnreadCount();
        showError(exception, "Không tải được thông báo");
    }

    private void showError(Exception exception, String fallbackMessage) {
        if (!isAdded()) {
            return;
        }

        String message = exception.getMessage();
        if (message == null || message.trim().isEmpty()) {
            message = fallbackMessage;
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}
