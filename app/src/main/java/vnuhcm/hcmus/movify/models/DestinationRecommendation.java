package vnuhcm.hcmus.movify.models;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;

import java.util.Locale;

public class DestinationRecommendation {
    private final String placeId;
    private final String name;
    private final String label;
    private final String address;
    private final String categoryLabel;
    private final String reason;
    private final double lat;
    private final double lng;
    private final double distanceMeters;
    private final double similarityScore;
    private final double recommendationScore;

    public DestinationRecommendation(String placeId,
                                     String name,
                                     String label,
                                     String address,
                                     String categoryLabel,
                                     String reason,
                                     double lat,
                                     double lng,
                                     double distanceMeters,
                                     double similarityScore,
                                     double recommendationScore) {
        this.placeId = placeId;
        this.name = name;
        this.label = label;
        this.address = address;
        this.categoryLabel = categoryLabel;
        this.reason = reason;
        this.lat = lat;
        this.lng = lng;
        this.distanceMeters = distanceMeters;
        this.similarityScore = similarityScore;
        this.recommendationScore = recommendationScore;
    }

    public String getPlaceId() {
        return placeId;
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public String getAddress() {
        return address;
    }

    public String getCategoryLabel() {
        return categoryLabel;
    }

    public String getReason() {
        return reason;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public double getDistanceMeters() {
        return distanceMeters;
    }

    public double getSimilarityScore() {
        return similarityScore;
    }

    public double getRecommendationScore() {
        return recommendationScore;
    }

    @NonNull
    public LatLng getLatLng() {
        return new LatLng(lat, lng);
    }

    @NonNull
    public String getRouteLabel() {
        String displayLabel = label == null || label.isBlank() ? name : label;
        return String.format(Locale.US, "%s (%.6f, %.6f)", displayLabel, lat, lng);
    }
}
