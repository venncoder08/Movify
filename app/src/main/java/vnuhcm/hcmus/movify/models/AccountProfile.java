package vnuhcm.hcmus.movify.models;

public class AccountProfile {
    private final int id;
    private final String fullName;
    private final String phoneNumber;
    private final String email;
    private final String role;
    private final String gender;
    private final String dateOfBirth;
    private final String avatar;
    private final String idCard;
    private final String driverLicense;
    private final String vehicleRegistration;
    private final String vehicleType;
    private final String driverStatus;

    public AccountProfile(int id,
                          String fullName,
                          String phoneNumber,
                          String email,
                          String role,
                          String gender,
                          String dateOfBirth,
                          String avatar,
                          String idCard,
                          String driverLicense,
                          String vehicleRegistration,
                          String vehicleType,
                          String driverStatus) {
        this.id = id;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.role = role;
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
        this.avatar = avatar;
        this.idCard = idCard;
        this.driverLicense = driverLicense;
        this.vehicleRegistration = vehicleRegistration;
        this.vehicleType = vehicleType;
        this.driverStatus = driverStatus;
    }

    public int getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getGender() {
        return gender;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public String getAvatar() {
        return avatar;
    }

    public String getIdCard() {
        return idCard;
    }

    public String getDriverLicense() {
        return driverLicense;
    }

    public String getVehicleRegistration() {
        return vehicleRegistration;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public String getDriverStatus() {
        return driverStatus;
    }
}
