package vnuhcm.hcmus.movify.models;

public class History {
    private int tripId;
    private String status;
    private String dateTime;
    private String pickupAddress;
    private String destinationAddress;
    private String driverName;
    private String driverPlate;
    private String duration;
    private String distance;
    private String price;
    private int vehicleIconResId;

    public History(int tripId, String status, String dateTime,
                       String pickupAddress, String destinationAddress,
                       String driverName, String driverPlate,
                       String duration, String distance, String price,
                       int vehicleIconResId) {
        this.tripId = tripId;
        this.status = status;
        this.dateTime = dateTime;
        this.pickupAddress = pickupAddress;
        this.destinationAddress = destinationAddress;
        this.driverName = driverName;
        this.driverPlate = driverPlate;
        this.duration = duration;
        this.distance = distance;
        this.price = price;
        this.vehicleIconResId = vehicleIconResId;
    }

    public int getTripId() {
        return tripId;
    }

    public String getStatus() {
        return status;
    }

    public String getDateTime() {
        return dateTime;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public String getDestinationAddress() {
        return destinationAddress;
    }

    public String getDriverName() {
        return driverName;
    }

    public String getDriverPlate() {
        return driverPlate;
    }

    public String getDuration() {
        return duration;
    }

    public String getDistance() {
        return distance;
    }

    public String getPrice() {
        return price;
    }

    public int getVehicleIconResId() {
        return vehicleIconResId;
    }
}