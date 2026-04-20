package vnuhcm.hcmus.movify.models;

import com.google.android.gms.maps.model.LatLng;

public class PlaceDetails {
    public final String label;
    public final LatLng latLng;

    public PlaceDetails(String label, LatLng latLng) {
        this.label = label;
        this.latLng = latLng;
    }
}
