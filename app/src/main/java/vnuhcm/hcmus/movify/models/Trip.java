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

    public Trip() {
    }

    public Trip(String id, double pickupLat, double pickupLng, String pickupAddress,
                double destinationLat, double destinationLng, String destinationAddress,
                double fare, String status, double distance) {
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
}
