package vnuhcm.hcmus.movify.models;

public class ChatMessage {
    private final int id;
    private final Integer tripId;
    private final String content;
    private final String contentType;
    private final String mediaUrl;
    private final boolean isRead;
    private final String createdAt;
    private final boolean isMine;

    public ChatMessage(int id,
                       Integer tripId,
                       String content,
                       String contentType,
                       String mediaUrl,
                       boolean isRead,
                       String createdAt,
                       boolean isMine) {
        this.id = id;
        this.tripId = tripId;
        this.content = content;
        this.contentType = contentType;
        this.mediaUrl = mediaUrl;
        this.isRead = isRead;
        this.createdAt = createdAt;
        this.isMine = isMine;
    }

    public int getId() {
        return id;
    }

    public Integer getTripId() {
        return tripId;
    }

    public String getContent() {
        return content;
    }

    public String getContentType() {
        return contentType;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public boolean isRead() {
        return isRead;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public boolean isMine() {
        return isMine;
    }

    public boolean isImage() {
        return "Image".equalsIgnoreCase(contentType);
    }
}
