package vnuhcm.hcmus.movify.utils;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public final class CurrentLocationResolver {

    private CurrentLocationResolver() {
    }

    @NonNull
    public static String resolveCurrentLocationLabel(Context context) throws IOException {
        Location location = getBestLastKnownLocation(context);
        if (location == null) {
            throw new IOException("Không lấy được vị trí hiện tại");
        }

        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        List<Address> addresses = geocoder.getFromLocation(
                location.getLatitude(),
                location.getLongitude(),
                1
        );

        if (addresses != null && !addresses.isEmpty()) {
            Address address = addresses.get(0);
            StringBuilder builder = new StringBuilder();
            for (int index = 0; index <= address.getMaxAddressLineIndex(); index++) {
                String line = address.getAddressLine(index);
                if (line == null || line.trim().isEmpty()) {
                    continue;
                }

                if (builder.length() > 0) {
                    builder.append(", ");
                }
                builder.append(line.trim());
            }

            if (builder.length() > 0) {
                return builder.toString();
            }
        }

        return String.format(Locale.getDefault(),
                "Vị trí hiện tại (%.5f, %.5f)",
                location.getLatitude(),
                location.getLongitude());
    }

    private static Location getBestLastKnownLocation(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            return null;
        }

        Location bestLocation = null;
        String[] providers = new String[] {
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
        };

        for (String provider : providers) {
            try {
                Location candidate = locationManager.getLastKnownLocation(provider);
                if (candidate == null) {
                    continue;
                }

                if (bestLocation == null || candidate.getTime() > bestLocation.getTime()) {
                    bestLocation = candidate;
                }
            } catch (SecurityException ignored) {
                return null;
            }
        }

        return bestLocation;
    }
}