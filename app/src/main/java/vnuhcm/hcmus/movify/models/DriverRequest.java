package vnuhcm.hcmus.movify.models;

public class DriverRequest {
    private final int id;
    private final String idCardImageUrl;
    private final String driverLicenseImageUrl;
    private final String vehicleRegistrationImageUrl;
    private final String vehicleType;
    private final String status;
    private final String createdAt;
    private final String updatedAt;
    private final String driverName;
    private final String driverEmail;
    private final String driverPhoneNumber;
    private final String driverAvatar;

    public DriverRequest(int id,
                         String idCardImageUrl,
                         String driverLicenseImageUrl,
                         String vehicleRegistrationImageUrl,
                         String vehicleType,
                         String status,
                         String createdAt,
                         String updatedAt,
                         String driverName,
                         String driverEmail,
                         String driverPhoneNumber,
                         String driverAvatar) {
        this.id = id;
        this.idCardImageUrl = idCardImageUrl;
        this.driverLicenseImageUrl = driverLicenseImageUrl;
        this.vehicleRegistrationImageUrl = vehicleRegistrationImageUrl;
        this.vehicleType = vehicleType;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.driverName = driverName;
        this.driverEmail = driverEmail;
        this.driverPhoneNumber = driverPhoneNumber;
        this.driverAvatar = driverAvatar;
    }

    public int getId() {
        return id;
    }

    public String getIdCardImageUrl() {
        return idCardImageUrl;
    }

    public String getDriverLicenseImageUrl() {
        return driverLicenseImageUrl;
    }

    public String getVehicleRegistrationImageUrl() {
        return vehicleRegistrationImageUrl;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public String getStatus() {
        return status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public String getDriverName() {
        return driverName;
    }

    public String getDriverEmail() {
        return driverEmail;
    }

    public String getDriverPhoneNumber() {
        return driverPhoneNumber;
    }

    public String getDriverAvatar() {
        return driverAvatar;
    }

    public boolean isPending() {
        return "Pending".equalsIgnoreCase(status);
    }
}
