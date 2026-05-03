package vnuhcm.hcmus.movify.models;

public class Notification {
    private int id;
    private int iconResId;
    private String title;
    private String message;
    private String time;
    private String category;
    private boolean isUnread;
    private boolean isWarning;

    public Notification(int id, int iconResId, String title, String message,
                            String time, String category,
                            boolean isUnread, boolean isWarning) {
        this.id = id;
        this.iconResId = iconResId;
        this.title = title;
        this.message = message;
        this.time = time;
        this.category = category;
        this.isUnread = isUnread;
        this.isWarning = isWarning;
    }

    public int getId() {
        return id;
    }

    public int getIconResId() {
        return iconResId;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getTime() {
        return time;
    }

    public String getCategory() {
        return category;
    }

    public boolean isUnread() {
        return isUnread;
    }

    public boolean isWarning() {
        return isWarning;
    }

    public void setUnread(boolean unread) {
        isUnread = unread;
    }
}
