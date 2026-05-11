package vnuhcm.hcmus.movify.models;

public class Trip {
    private String id;
    private double pickupLat;
    private double pickupLng;
    private String pickupAddress;
    private double destinationLat;
    private double destinationLng;
    private String destinationAddress;
    private double fare;
    private String status; // pending, accepted, completed, cancelled
    private double distance; // distance from driver's current location in km
    private String tripType; // Motor or Taxi
    private boolean driverPickupConfirmed;
    private boolean passengerPickupConfirmed;
    private double driverCurrentLat;
    private double driverCurrentLng;
    private String driverLocationUpdatedAt;
    private int driverId;
    private String driverFullName;
    private String driverPhoneNumber;
    private int passengerId;
    private String passengerFullName;
    private String passengerPhoneNumber;

    public Trip() {
    }

    public Trip(String id, double pickupLat, double pickupLng, String pickupAddress,
                double destinationLat, double destinationLng, String destinationAddress,
                double fare, String status, double distance) {
        this(id, pickupLat, pickupLng, pickupAddress, destinationLat, destinationLng,
                destinationAddress, fare, status, distance, "");
    }

    public Trip(String id, double pickupLat, double pickupLng, String pickupAddress,
                double destinationLat, double destinationLng, String destinationAddress,
                double fare, String status, double distance, String tripType) {
        this.id = id;
        this.pickupLat = pickupLat;
        this.pickupLng = pickupLng;
        this.pickupAddress = pickupAddress;
        this.destinationLat = destinationLat;
        this.destinationLng = destinationLng;
        this.destinationAddress = destinationAddress;
        this.fare = fare;
        this.status = status;
        this.distance = distance;
        this.tripType = tripType;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getPickupLat() {
        return pickupLat;
    }

    public void setPickupLat(double pickupLat) {
        this.pickupLat = pickupLat;
    }

    public double getPickupLng() {
        return pickupLng;
    }

    public void setPickupLng(double pickupLng) {
        this.pickupLng = pickupLng;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public double getDestinationLat() {
        return destinationLat;
    }

    public void setDestinationLat(double destinationLat) {
        this.destinationLat = destinationLat;
    }

    public double getDestinationLng() {
        return destinationLng;
    }

    public void setDestinationLng(double destinationLng) {
        this.destinationLng = destinationLng;
    }

    public String getDestinationAddress() {
        return destinationAddress;
    }

    public void setDestinationAddress(String destinationAddress) {
        this.destinationAddress = destinationAddress;
    }

    public double getFare() {
        return fare;
    }

    public void setFare(double fare) {
        this.fare = fare;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public String getTripType() {
        return tripType;
    }

    public void setTripType(String tripType) {
        this.tripType = tripType;
    }

    public boolean isDriverPickupConfirmed() {
        return driverPickupConfirmed;
    }

    public void setDriverPickupConfirmed(boolean driverPickupConfirmed) {
        this.driverPickupConfirmed = driverPickupConfirmed;
    }

    public boolean isPassengerPickupConfirmed() {
        return passengerPickupConfirmed;
    }

    public void setPassengerPickupConfirmed(boolean passengerPickupConfirmed) {
        this.passengerPickupConfirmed = passengerPickupConfirmed;
    }

    public double getDriverCurrentLat() {
        return driverCurrentLat;
    }

    public void setDriverCurrentLat(double driverCurrentLat) {
        this.driverCurrentLat = driverCurrentLat;
    }

    public double getDriverCurrentLng() {
        return driverCurrentLng;
    }

    public void setDriverCurrentLng(double driverCurrentLng) {
        this.driverCurrentLng = driverCurrentLng;
    }

    public String getDriverLocationUpdatedAt() {
        return driverLocationUpdatedAt;
    }

    public void setDriverLocationUpdatedAt(String driverLocationUpdatedAt) {
        this.driverLocationUpdatedAt = driverLocationUpdatedAt;
    }

    public String getDriverFullName() {
        return driverFullName;
    }

    public void setDriverFullName(String driverFullName) {
        this.driverFullName = driverFullName;
    }

    public String getDriverPhoneNumber() {
        return driverPhoneNumber;
    }

    public void setDriverPhoneNumber(String driverPhoneNumber) {
        this.driverPhoneNumber = driverPhoneNumber;
    }

    public int getDriverId() {
        return driverId;
    }

    public void setDriverId(int driverId) {
        this.driverId = driverId;
    }

    public int getPassengerId() {
        return passengerId;
    }

    public void setPassengerId(int passengerId) {
        this.passengerId = passengerId;
    }

    public String getPassengerFullName() {
        return passengerFullName;
    }

    public void setPassengerFullName(String passengerFullName) {
        this.passengerFullName = passengerFullName;
    }

    public String getPassengerPhoneNumber() {
        return passengerPhoneNumber;
    }

    public void setPassengerPhoneNumber(String passengerPhoneNumber) {
        this.passengerPhoneNumber = passengerPhoneNumber;
    }

    public boolean hasDriverLocation() {
        return isValidCoordinate(driverCurrentLat, driverCurrentLng);
    }

    private boolean isValidCoordinate(double lat, double lng) {
        return lat >= -90d && lat <= 90d
                && lng >= -180d && lng <= 180d
                && !(Math.abs(lat) < 0.000001d && Math.abs(lng) < 0.000001d);
    }
}
