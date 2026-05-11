package vnuhcm.hcmus.movify.models;

public class ConversationSummary {
    private final ChatParticipant participant;
    private final String lastMessagePreview;
    private final String updatedAt;
    private final int unreadCount;
    private final Integer activeTripId;
    private final String activeTripStatus;
    private final Integer lastTripId;
    private final boolean canSend;

    public ConversationSummary(ChatParticipant participant,
                               String lastMessagePreview,
                               String updatedAt,
                               int unreadCount,
                               Integer activeTripId,
                               String activeTripStatus,
                               Integer lastTripId,
                               boolean canSend) {
        this.participant = participant;
        this.lastMessagePreview = lastMessagePreview;
        this.updatedAt = updatedAt;
        this.unreadCount = unreadCount;
        this.activeTripId = activeTripId;
        this.activeTripStatus = activeTripStatus;
        this.lastTripId = lastTripId;
        this.canSend = canSend;
    }

    public ChatParticipant getParticipant() {
        return participant;
    }

    public String getLastMessagePreview() {
        return lastMessagePreview;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public Integer getActiveTripId() {
        return activeTripId;
    }

    public String getActiveTripStatus() {
        return activeTripStatus;
    }

    public Integer getLastTripId() {
        return lastTripId;
    }

    public boolean canSend() {
        return canSend;
    }
}
