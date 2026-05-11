package vnuhcm.hcmus.movify.models;

import java.util.List;

public class TripConversationDetail {
    private final int tripId;
    private final String tripStatus;
    private final Integer participantId;
    private final List<ChatMessage> messages;

    public TripConversationDetail(int tripId, String tripStatus, Integer participantId, List<ChatMessage> messages) {
        this.tripId = tripId;
        this.tripStatus = tripStatus;
        this.participantId = participantId;
        this.messages = messages;
    }

    public int getTripId() {
        return tripId;
    }

    public String getTripStatus() {
        return tripStatus;
    }

    public Integer getParticipantId() {
        return participantId;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }
}
