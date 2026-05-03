package vnuhcm.hcmus.movify.utils;

public class LocationData {
    private static String pickupLocation = "";
    private static String destinationLocation = "";

    public static String getPickupLocation() {
        return pickupLocation;
    }

    public static void setPickupLocation(String location) {
        pickupLocation = location;
    }

    public static String getDestinationLocation() {
        return destinationLocation;
    }

    public static void setDestinationLocation(String location) {
        destinationLocation = location;
    }

    public static void clear() {
        pickupLocation = "";
        destinationLocation = "";
    }
}
