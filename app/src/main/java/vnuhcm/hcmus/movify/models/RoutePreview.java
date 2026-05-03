package vnuhcm.hcmus.movify.models;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public class RoutePreview {
    public final PlaceDetails pickup;
    public final PlaceDetails destination;
    public final List<LatLng> routePoints;
    public final int distanceMeters;
    public final double durationSeconds;

    public RoutePreview(PlaceDetails pickup,
                        PlaceDetails destination,
                        List<LatLng> routePoints,
                        int distanceMeters,
                        double durationSeconds) {
        this.pickup = pickup;
        this.destination = destination;
        this.routePoints = routePoints;
        this.distanceMeters = distanceMeters;
        this.durationSeconds = durationSeconds;
    }
}
