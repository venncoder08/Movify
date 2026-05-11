package vnuhcm.hcmus.movify.models;

public class ChatParticipant {
    private final int id;
    private final String fullName;
    private final String avatar;
    private final String phoneNumber;

    public ChatParticipant(int id, String fullName, String avatar, String phoneNumber) {
        this.id = id;
        this.fullName = fullName;
        this.avatar = avatar;
        this.phoneNumber = phoneNumber;
    }

    public int getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getAvatar() {
        return avatar;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }
}
