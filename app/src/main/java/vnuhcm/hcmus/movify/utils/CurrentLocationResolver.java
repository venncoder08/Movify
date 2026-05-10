package vnuhcm.hcmus.movify.utils;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.Tasks;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

public final class CurrentLocationResolver {

    private static final int GEOCODER_RESULT_LIMIT = 5;
    private static final int COORDINATE_PRECISION = 6;
    private static final long FRESH_LOCATION_TIMEOUT_SECONDS = 8L;
    private static final long MAX_LAST_KNOWN_AGE_MS = TimeUnit.MINUTES.toMillis(10);
    private static final long SIGNIFICANT_TIME_DELTA_MS = TimeUnit.MINUTES.toMillis(2);
    private static final String CURRENT_LOCATION_PREFIX = "Vị trí hiện tại";
    private static final String MAP_SELECTED_LOCATION_LABEL = "Vị trí đã chọn trên bản đồ";
    private static final Pattern TRAILING_COORDINATES_PATTERN = Pattern.compile(
            "\\s*\\(-?\\d{1,2}\\.\\d{3,}\\s*,\\s*-?\\d{1,3}\\.\\d{3,}\\)\\s*$"
    );

    private CurrentLocationResolver() {
    }

    @NonNull
    public static String resolveCurrentLocationLabel(Context context) throws IOException {
        Location location = getFreshHighAccuracyLocation(context);
        if (location == null) {
            location = getBestLastKnownLocation(context);
        }

        if (location == null) {
            throw new IOException("Không lấy được vị trí hiện tại");
        }

        String addressLabel = resolveAddressLabel(context, location);
        String coordinates = formatCoordinates(location);
        if (!TextUtils.isEmpty(addressLabel)) {
            return "Vị trí hiện tại - " + addressLabel + " " + coordinates;
        }

        return "Vị trí hiện tại " + coordinates;
    }

    @NonNull
    public static String getShareableLocationLabel(@Nullable String rawLabel) {
        if (TextUtils.isEmpty(rawLabel)) {
            return "";
        }

        String label = TRAILING_COORDINATES_PATTERN.matcher(rawLabel.trim()).replaceFirst("").trim();
        if (label.toLowerCase(Locale.ROOT).startsWith(CURRENT_LOCATION_PREFIX.toLowerCase(Locale.ROOT))) {
            label = label.substring(CURRENT_LOCATION_PREFIX.length()).trim();
            if (label.startsWith("-") || label.startsWith(":")) {
                label = label.substring(1).trim();
            }
        }

        return TextUtils.isEmpty(label) ? MAP_SELECTED_LOCATION_LABEL : label;
    }

    @Nullable
    private static Location getFreshHighAccuracyLocation(Context context) {
        try {
            return Tasks.await(
                    LocationServices.getFusedLocationProviderClient(context)
                            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null),
                    FRESH_LOCATION_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS
            );
        } catch (SecurityException exception) {
            return null;
        } catch (ExecutionException | TimeoutException exception) {
            return null;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    @Nullable
    private static String resolveAddressLabel(Context context, Location location) throws IOException {
        Geocoder geocoder = new Geocoder(context, new Locale("vi", "VN"));
        List<Address> addresses = geocoder.getFromLocation(
                location.getLatitude(),
                location.getLongitude(),
                GEOCODER_RESULT_LIMIT
        );

        if (addresses == null || addresses.isEmpty()) {
            return null;
        }

        for (Address address : addresses) {
            String componentAddress = buildComponentAddress(address);
            if (looksLikeStreetAddress(componentAddress)) {
                return componentAddress;
            }
        }

        for (Address address : addresses) {
            String addressLine = buildAddressLines(address);
            if (looksLikeStreetAddress(addressLine)) {
                return addressLine;
            }
        }

        for (Address address : addresses) {
            String componentAddress = buildComponentAddress(address);
            if (!TextUtils.isEmpty(componentAddress)) {
                return componentAddress;
            }
        }

        for (Address address : addresses) {
            String addressLine = buildAddressLines(address);
            if (!TextUtils.isEmpty(addressLine)) {
                return addressLine;
            }
        }

        return null;
    }

    @NonNull
    private static String buildComponentAddress(Address address) {
        List<String> parts = new ArrayList<>();
        addIfPresent(parts, address.getSubThoroughfare());
        addIfPresent(parts, address.getThoroughfare());
        addIfPresent(parts, address.getSubLocality());
        addIfPresent(parts, address.getLocality());
        addIfPresent(parts, address.getSubAdminArea());
        addIfPresent(parts, address.getAdminArea());
        addIfPresent(parts, address.getCountryName());
        return joinUnique(parts);
    }

    @NonNull
    private static String buildAddressLines(Address address) {
        List<String> parts = new ArrayList<>();
        for (int index = 0; index <= address.getMaxAddressLineIndex(); index++) {
            addIfPresent(parts, address.getAddressLine(index));
        }
        return joinUnique(parts);
    }

    private static void addIfPresent(List<String> parts, @Nullable String value) {
        if (TextUtils.isEmpty(value)) {
            return;
        }

        String trimmed = value.trim();
        if (!trimmed.isEmpty()) {
            parts.add(trimmed);
        }
    }

    @NonNull
    private static String joinUnique(List<String> parts) {
        Set<String> uniqueParts = new LinkedHashSet<>();
        for (String part : parts) {
            if (!TextUtils.isEmpty(part)) {
                uniqueParts.add(part.trim());
            }
        }
        return TextUtils.join(", ", uniqueParts);
    }

    private static boolean looksLikeStreetAddress(@Nullable String value) {
        if (TextUtils.isEmpty(value)) {
            return false;
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);

        return normalized.matches(".*\\d.*")
                || normalized.contains("hem")
                || normalized.contains("ngo")
                || normalized.contains("ngach")
                || normalized.contains("duong")
                || normalized.contains("pho")
                || normalized.contains("street")
                || normalized.contains("alley");
    }

    @NonNull
    private static String formatCoordinates(Location location) {
        return String.format(
                Locale.US,
                "(%." + COORDINATE_PRECISION + "f, %." + COORDINATE_PRECISION + "f)",
                location.getLatitude(),
                location.getLongitude()
        );
    }

    @Nullable
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
                if (candidate == null || isTooOld(candidate)) {
                    continue;
                }

                if (isBetterLocation(candidate, bestLocation)) {
                    bestLocation = candidate;
                }
            } catch (SecurityException ignored) {
                return null;
            }
        }

        return bestLocation;
    }

    private static boolean isTooOld(Location location) {
        long ageMs = System.currentTimeMillis() - location.getTime();
        return ageMs > MAX_LAST_KNOWN_AGE_MS;
    }

    private static boolean isBetterLocation(@NonNull Location candidate, @Nullable Location currentBest) {
        if (currentBest == null) {
            return true;
        }

        long timeDelta = candidate.getTime() - currentBest.getTime();
        boolean significantlyNewer = timeDelta > SIGNIFICANT_TIME_DELTA_MS;
        boolean significantlyOlder = timeDelta < -SIGNIFICANT_TIME_DELTA_MS;
        boolean newer = timeDelta > 0;

        if (significantlyNewer) {
            return true;
        }
        if (significantlyOlder) {
            return false;
        }

        int accuracyDelta = Math.round(candidate.getAccuracy() - currentBest.getAccuracy());
        boolean moreAccurate = accuracyDelta < 0;
        boolean significantlyLessAccurate = accuracyDelta > 200;
        boolean sameProvider = TextUtils.equals(candidate.getProvider(), currentBest.getProvider());

        return moreAccurate || (newer && !significantlyLessAccurate) || (newer && sameProvider);
    }
}
