package vnuhcm.hcmus.movify.models;

public class DriverRequestStatus {
    private final String accountRole;
    private final String driverStatus;
    private final String idCard;
    private final String driverLicense;
    private final String vehicleRegistration;
    private final String vehicleType;
    private final String latestRequestStatus;
    private final String submittedAt;

    public DriverRequestStatus(String accountRole,
                               String driverStatus,
                               String idCard,
                               String driverLicense,
                               String vehicleRegistration,
                               String vehicleType,
                               String latestRequestStatus,
                               String submittedAt) {
        this.accountRole = accountRole;
        this.driverStatus = driverStatus;
        this.idCard = idCard;
        this.driverLicense = driverLicense;
        this.vehicleRegistration = vehicleRegistration;
        this.vehicleType = vehicleType;
        this.latestRequestStatus = latestRequestStatus;
        this.submittedAt = submittedAt;
    }

    public String getAccountRole() {
        return accountRole;
    }

    public String getDriverStatus() {
        return driverStatus;
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

    public String getLatestRequestStatus() {
        return latestRequestStatus;
    }

    public String getSubmittedAt() {
        return submittedAt;
    }
}
