package vnuhcm.hcmus.movify.models;

import java.util.List;

public class NotificationFeed {
    private final List<Notification> notifications;
    private final int unreadCount;

    public NotificationFeed(List<Notification> notifications, int unreadCount) {
        this.notifications = notifications;
        this.unreadCount = unreadCount;
    }

    public List<Notification> getNotifications() {
        return notifications;
    }

    public int getUnreadCount() {
        return unreadCount;
    }
}
