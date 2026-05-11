package vnuhcm.hcmus.movify.models;

import java.util.List;

public class ConversationDetail {
    private final ChatParticipant participant;
    private final List<ChatMessage> messages;
    private final boolean canSend;
    private final Integer activeTripId;
    private final String activeTripStatus;

    public ConversationDetail(ChatParticipant participant,
                              List<ChatMessage> messages,
                              boolean canSend,
                              Integer activeTripId,
                              String activeTripStatus) {
        this.participant = participant;
        this.messages = messages;
        this.canSend = canSend;
        this.activeTripId = activeTripId;
        this.activeTripStatus = activeTripStatus;
    }

    public ChatParticipant getParticipant() {
        return participant;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public boolean canSend() {
        return canSend;
    }

    public Integer getActiveTripId() {
        return activeTripId;
    }

    public String getActiveTripStatus() {
        return activeTripStatus;
    }
}
