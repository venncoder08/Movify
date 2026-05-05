package vnuhcm.hcmus.movify.fragments;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vnuhcm.hcmus.movify.R;
import vnuhcm.hcmus.movify.models.Trip;
import vnuhcm.hcmus.movify.adapters.DriverTripAdapter;
import vnuhcm.hcmus.movify.services.DriverTripService;

public class DriverMapFragment extends Fragment implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final String TAG = "DriverMapFragment";
    private static final LatLng HCMC = new LatLng(10.8231, 106.6297);
    private static final int CAMERA_ZOOM = 13;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Map components
    private GoogleMap mMap;
    private SupportMapFragment mapFragment;

    // UI components
    private RecyclerView tripsRecyclerView;
    private Button btnRefreshTrips;
    private Button btnAcceptTrip;
    private ProgressBar loadingProgressBar;
    private TextView emptyTripsText;

    // Data
    private DriverTripAdapter tripsAdapter;
    private List<Trip> nearbyTrips = new ArrayList<>();
    private Trip selectedTrip = null;
    private LatLng driverLocation = null;

    // Threading
    private ExecutorService dataExecutor;

    public DriverMapFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_driver_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize UI components
        tripsRecyclerView = view.findViewById(R.id.tripsRecyclerView);
        btnRefreshTrips = view.findViewById(R.id.btnRefreshTrips);
        btnAcceptTrip = view.findViewById(R.id.btnAcceptTrip);
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar);
        emptyTripsText = view.findViewById(R.id.emptyTripsText);

        dataExecutor = Executors.newSingleThreadExecutor();

        // Setup RecyclerView
        setupTripsRecyclerView();

        // Setup button listeners
        btnRefreshTrips.setOnClickListener(v -> refreshNearbyTrips());
        btnAcceptTrip.setOnClickListener(v -> acceptSelectedTrip());

        // Request location permission and get map
        requestLocationPermission();

        mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.mapDriver);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mainHandler.removeCallbacksAndMessages(null);
        if (dataExecutor != null) {
            dataExecutor.shutdownNow();
            dataExecutor = null;
        }

        mMap = null;
        mapFragment = null;
        tripsAdapter = null;
    }

    private void setupTripsRecyclerView() {
        tripsAdapter = new DriverTripAdapter(nearbyTrips, trip -> {
            selectedTrip = trip;
            updateSelectedTripUI();
            updateMapMarkers();
        });

        tripsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));
        tripsRecyclerView.setAdapter(tripsAdapter);
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && mMap != null) {
            enableMyLocation();
            refreshNearbyTrips();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(HCMC, CAMERA_ZOOM));
        enableMyLocation();
        // Auto-load trips as soon as the map is ready
        refreshNearbyTrips();
    }

    private void enableMyLocation() {
        if (mMap == null) {
            return;
        }

        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
    }

    private void refreshNearbyTrips() {
        if (dataExecutor == null || dataExecutor.isShutdown()) {
            return;
        }

        showLoading(true);

        dataExecutor.execute(() -> {
            try {
                // Get current driver location; use HCMC center as default if not available
                LatLng currentLocation = driverLocation != null ? driverLocation : HCMC;
                double lat = currentLocation.latitude;
                double lng = currentLocation.longitude;
                // Large radius so address-only trips (no coordinates) always appear;
                // the backend returns them regardless of distance.
                double radius = 50.0;

                // Fetch nearby trips from backend
                List<Trip> trips = DriverTripService.getNearbyTrips(
                        requireContext(),
                        lat,
                        lng,
                        radius
                );

                mainHandler.post(() -> {
                    nearbyTrips.clear();
                    nearbyTrips.addAll(trips);
                    updateTripsUI();
                    updateMapMarkers();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error fetching nearby trips", e);
                mainHandler.post(() -> {
                    showLoading(false);
                    Toast.makeText(requireContext(), "Failed to load trips: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void updateTripsUI() {
        showLoading(false);

        if (nearbyTrips.isEmpty()) {
            tripsRecyclerView.setVisibility(View.GONE);
            emptyTripsText.setVisibility(View.VISIBLE);
            btnAcceptTrip.setEnabled(false);
            selectedTrip = null;
        } else {
            tripsRecyclerView.setVisibility(View.VISIBLE);
            emptyTripsText.setVisibility(View.GONE);
            tripsAdapter.notifyDataSetChanged();
            if (!nearbyTrips.isEmpty()) {
                selectedTrip = nearbyTrips.get(0);
                updateSelectedTripUI();
            }
        }
    }

    private void updateSelectedTripUI() {
        if (selectedTrip != null) {
            btnAcceptTrip.setEnabled(true);
        } else {
            btnAcceptTrip.setEnabled(false);
        }
    }

    private void updateMapMarkers() {
        if (mMap == null) {
            return;
        }

        mMap.clear();

        // Add marker for selected trip's pickup location
        if (selectedTrip != null) {
            LatLng pickupLocation = new LatLng(selectedTrip.getPickupLat(), selectedTrip.getPickupLng());
            mMap.addMarker(new MarkerOptions()
                    .position(pickupLocation)
                    .title("Pickup: " + selectedTrip.getPickupAddress())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

            // Animate camera to show pickup and driver location
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pickupLocation, CAMERA_ZOOM));
        }
    }

    private void acceptSelectedTrip() {
        if (selectedTrip == null) {
            Toast.makeText(requireContext(), "No trip selected", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        dataExecutor.execute(() -> {
            try {
                int tripId = Integer.parseInt(selectedTrip.getId());
                Trip acceptedTrip = DriverTripService.acceptTrip(requireContext(), tripId);

                mainHandler.post(() -> {
                    showLoading(false);
                    Toast.makeText(requireContext(), "Trip accepted successfully!", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Trip accepted: " + acceptedTrip.getId());

                    // Optionally: remove the accepted trip from list and refresh
                    nearbyTrips.remove(selectedTrip);
                    tripsAdapter.notifyDataSetChanged();
                    updateTripsUI();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error accepting trip", e);
                mainHandler.post(() -> {
                    showLoading(false);
                    Toast.makeText(requireContext(), "Failed to accept trip: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showLoading(boolean show) {
        loadingProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
