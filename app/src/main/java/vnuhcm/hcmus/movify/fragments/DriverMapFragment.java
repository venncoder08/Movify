package vnuhcm.hcmus.movify.fragments;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
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
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.Tasks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import vnuhcm.hcmus.movify.R;
import vnuhcm.hcmus.movify.models.RoutePreview;
import vnuhcm.hcmus.movify.models.Trip;
import vnuhcm.hcmus.movify.adapters.DriverTripAdapter;
import vnuhcm.hcmus.movify.services.DriverTripService;
import vnuhcm.hcmus.movify.services.RouteService;
import vnuhcm.hcmus.movify.services.TripService;
import vnuhcm.hcmus.movify.utils.CurrentLocationResolver;

public class DriverMapFragment extends Fragment implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final String TAG = "DriverMapFragment";
    private static final LatLng HCMC = new LatLng(10.8231, 106.6297);
    private static final int CAMERA_ZOOM = 13;
    private static final int ROUTE_PADDING_PX = 120;
    private static final long LOCATION_TIMEOUT_SECONDS = 8L;
    private static final long DRIVER_LOCATION_UPDATE_INTERVAL_MS = 5000L;
    private static final long ACCEPTED_TRIP_POLL_INTERVAL_MS = 5000L;
    private static final double PICKUP_CONFIRM_RADIUS_METERS = 50d;
    private static final double DESTINATION_COMPLETE_RADIUS_METERS = 50d;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Map components
    private GoogleMap mMap;
    private SupportMapFragment mapFragment;

    // UI components
    private RecyclerView tripsRecyclerView;
    private Button btnRefreshTrips;
    private Button btnAcceptTrip;
    private Button btnCancelTrip;
    private ProgressBar loadingProgressBar;
    private TextView emptyTripsText;

    // Data
    private DriverTripAdapter tripsAdapter;
    private List<Trip> nearbyTrips = new ArrayList<>();
    private final List<LatLng> routeToPickupPoints = new ArrayList<>();
    private Trip selectedTrip = null;
    private LatLng driverLocation = null;
    private boolean navigatingToPickup = false;
    private boolean routingToDestination = false;
    private Runnable pendingDriverLocationUpdate;
    private Runnable pendingAcceptedTripPoll;

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
        btnCancelTrip = view.findViewById(R.id.btnCancelTrip);
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar);
        emptyTripsText = view.findViewById(R.id.emptyTripsText);

        dataExecutor = Executors.newSingleThreadExecutor();

        // Setup RecyclerView
        setupTripsRecyclerView();

        // Setup button listeners
        btnRefreshTrips.setOnClickListener(v -> refreshNearbyTrips());
        btnAcceptTrip.setOnClickListener(v -> handlePrimaryAction());
        btnCancelTrip.setOnClickListener(v -> showCancelAcceptedTripDialog());

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
            loadActiveTripOrNearby();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(HCMC, CAMERA_ZOOM));
        enableMyLocation();
        loadActiveTripOrNearby();
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
        if (navigatingToPickup) {
            Toast.makeText(requireContext(), "Bạn đang trong chuyến đã nhận", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        Context appContext = requireContext().getApplicationContext();

        dataExecutor.execute(() -> {
            try {
                LatLng freshLocation = resolveCurrentDriverLocation(appContext);
                LatLng currentLocation = freshLocation != null
                        ? freshLocation
                        : (driverLocation != null ? driverLocation : HCMC);
                double lat = currentLocation.latitude;
                double lng = currentLocation.longitude;
                // Large radius so address-only trips (no coordinates) always appear;
                // the backend returns them regardless of distance.
                double radius = 50.0;

                // Fetch nearby trips from backend
                List<Trip> trips = DriverTripService.getNearbyTrips(
                        appContext,
                        lat,
                        lng,
                        radius
                );

                mainHandler.post(() -> {
                    driverLocation = currentLocation;
                    nearbyTrips.clear();
                    nearbyTrips.addAll(trips);
                    updateTripsUI();
                    updateMapMarkers();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error fetching nearby trips", e);
                mainHandler.post(() -> {
                    showLoading(false);
                    Toast.makeText(requireContext(), "Không tải được chuyến: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void updateTripsUI() {
        showLoading(false);

        if (navigatingToPickup) {
            renderAcceptedTripPanel();
            return;
        }

        btnRefreshTrips.setEnabled(true);
        btnAcceptTrip.setText(R.string.accept_trip);
        btnCancelTrip.setVisibility(View.GONE);
        btnCancelTrip.setEnabled(false);

        if (nearbyTrips.isEmpty()) {
            tripsRecyclerView.setVisibility(View.GONE);
            emptyTripsText.setVisibility(View.VISIBLE);
            emptyTripsText.setText(R.string.no_nearby_trips);
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

    private void clearActiveTripAndRefresh() {
        if (pendingDriverLocationUpdate != null) {
            mainHandler.removeCallbacks(pendingDriverLocationUpdate);
            pendingDriverLocationUpdate = null;
        }
        if (pendingAcceptedTripPoll != null) {
            mainHandler.removeCallbacks(pendingAcceptedTripPoll);
            pendingAcceptedTripPoll = null;
        }

        navigatingToPickup = false;
        routingToDestination = false;
        selectedTrip = null;
        routeToPickupPoints.clear();
        btnCancelTrip.setVisibility(View.GONE);
        btnCancelTrip.setEnabled(false);
        nearbyTrips.clear();
        if (tripsAdapter != null) {
            tripsAdapter.notifyDataSetChanged();
        }
        if (mMap != null) {
            mMap.clear();
        }
        refreshNearbyTrips();
    }

    private void updateSelectedTripUI() {
        if (selectedTrip != null) {
            btnAcceptTrip.setEnabled(true);
        } else {
            btnAcceptTrip.setEnabled(false);
        }
    }

    private boolean isTripInProgress() {
        return routingToDestination
                || "InProgress".equalsIgnoreCase(selectedTrip == null ? "" : selectedTrip.getStatus());
    }

    private void handlePrimaryAction() {
        if (navigatingToPickup) {
            if (isTripInProgress()) {
                completeDriverTrip();
                return;
            }
            confirmDriverPickup();
            return;
        }

        acceptSelectedTrip();
    }

    private void renderAcceptedTripPanel() {
        tripsRecyclerView.setVisibility(View.GONE);
        emptyTripsText.setVisibility(View.VISIBLE);
        btnRefreshTrips.setEnabled(false);
        boolean canCancel = selectedTrip != null
                && "Accepted".equalsIgnoreCase(selectedTrip.getStatus())
                && !isTripInProgress();
        btnCancelTrip.setVisibility(canCancel ? View.VISIBLE : View.GONE);
        btnCancelTrip.setEnabled(canCancel);

        if (selectedTrip == null) {
            emptyTripsText.setText("Đã nhận chuyến.");
            btnAcceptTrip.setEnabled(false);
            return;
        }

        if (isTripInProgress()) {
            LatLng destinationLocation = getTripDestinationLatLng(selectedTrip);
            double distanceMeters = destinationLocation == null || driverLocation == null
                    ? Double.NaN
                    : calculateDistanceMeters(driverLocation, destinationLocation);
            boolean canCompleteTrip = !Double.isNaN(distanceMeters)
                    && distanceMeters <= DESTINATION_COMPLETE_RADIUS_METERS;

            if (Double.isNaN(distanceMeters)) {
                emptyTripsText.setText("Chuyến đi đang diễn ra. Đang cập nhật khoảng cách tới điểm đến.");
                btnAcceptTrip.setText("Đang cập nhật vị trí");
                btnAcceptTrip.setEnabled(false);
                return;
            }

            if (!Double.isNaN(distanceMeters)) {
                emptyTripsText.setText("Còn " + formatMeters(distanceMeters) + " tới điểm đến. Chỉ có thể hoàn thành trong phạm vi 50m.");
                btnAcceptTrip.setText(canCompleteTrip ? "Hoàn thành chuyến" : "Còn " + formatMeters(distanceMeters));
                btnAcceptTrip.setEnabled(canCompleteTrip);
                return;
            }
            emptyTripsText.setText("Chuyến đi đang diễn ra. Đi theo tuyến đến điểm đến.");
            btnAcceptTrip.setText("Đang trong chuyến");
            btnAcceptTrip.setEnabled(false);
            return;
        }

        if (selectedTrip.isDriverPickupConfirmed()) {
            emptyTripsText.setText("Đã xác nhận đón khách. Chờ khách xác nhận lên xe.");
            btnAcceptTrip.setText("Chờ khách xác nhận");
            btnAcceptTrip.setEnabled(false);
            return;
        }

        LatLng pickupLocation = getTripPickupLatLng(selectedTrip);
        double distanceMeters = pickupLocation == null || driverLocation == null
                ? Double.NaN
                : calculateDistanceMeters(driverLocation, pickupLocation);
        boolean canConfirmPickup = !Double.isNaN(distanceMeters)
                && distanceMeters <= PICKUP_CONFIRM_RADIUS_METERS;

        if (Double.isNaN(distanceMeters)) {
            emptyTripsText.setText("Đã nhận chuyến. Đang cập nhật khoảng cách tới điểm đón.");
            btnAcceptTrip.setText("Đang cập nhật vị trí");
            btnAcceptTrip.setEnabled(false);
            return;
        }

        emptyTripsText.setText("Cách điểm đón " + formatMeters(distanceMeters) + ". Chỉ có thể xác nhận trong phạm vi 50m.");
        btnAcceptTrip.setText(canConfirmPickup ? "Xác nhận đã đón" : "Còn " + formatMeters(distanceMeters));
        btnAcceptTrip.setEnabled(canConfirmPickup);
    }

    private void updateMapMarkers() {
        if (mMap == null) {
            return;
        }

        mMap.clear();
        if (navigatingToPickup) {
            if (routingToDestination || "InProgress".equalsIgnoreCase(selectedTrip == null ? "" : selectedTrip.getStatus())) {
                renderRouteToDestination();
            } else {
                renderRouteToPickup();
            }
            return;
        }

        // Add marker for selected trip's pickup location
        if (selectedTrip != null) {
            LatLng pickupLocation = getTripPickupLatLng(selectedTrip);
            if (pickupLocation == null) {
                return;
            }

            addPickupMarker(pickupLocation, selectedTrip.getPickupAddress());
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pickupLocation, CAMERA_ZOOM));
        }
    }

    private void acceptSelectedTrip() {
        if (selectedTrip == null) {
            Toast.makeText(requireContext(), "Chưa chọn chuyến", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        Context appContext = requireContext().getApplicationContext();
        Trip tripToAccept = selectedTrip;

        dataExecutor.execute(() -> {
            try {
                int tripId = Integer.parseInt(tripToAccept.getId());
                Trip acceptedTrip = DriverTripService.acceptTrip(appContext, tripId);
                LatLng freshDriverLocation = resolveCurrentDriverLocation(appContext);
                LatLng routeStart = freshDriverLocation != null ? freshDriverLocation : driverLocation;
                if (routeStart != null) {
                    try {
                        acceptedTrip = TripService.updateDriverLocation(
                                appContext,
                                acceptedTrip.getId(),
                                routeStart.latitude,
                                routeStart.longitude
                        );
                    } catch (Exception exception) {
                        Log.w(TAG, "Unable to publish driver location after accept", exception);
                    }
                }
                List<LatLng> routePoints = new ArrayList<>();
                Exception routeError = null;

                if (routeStart != null) {
                    try {
                        RoutePreview routePreview = RouteService.fetchRoutePreview(
                                appContext,
                                formatLatLng(routeStart),
                                buildPickupRouteQuery(acceptedTrip)
                        );
                        routePoints.addAll(routePreview.routePoints);
                    } catch (Exception exception) {
                        routeError = exception;
                        Log.w(TAG, "Unable to draw route to pickup", exception);
                    }
                } else {
                    routeError = new IOException("Không lấy được vị trí hiện tại của tài xế");
                }

                LatLng finalRouteStart = routeStart;
                Trip finalAcceptedTrip = acceptedTrip;
                Exception finalRouteError = routeError;

                mainHandler.post(() -> {
                    showLoading(false);
                    if (!isAdded()) {
                        return;
                    }

                    Toast.makeText(requireContext(), "Đã nhận chuyến", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Trip accepted: " + finalAcceptedTrip.getId());

                    selectedTrip = finalAcceptedTrip;
                    navigatingToPickup = true;
                    routingToDestination = false;
                    if (finalRouteStart != null) {
                        driverLocation = finalRouteStart;
                    }
                    routeToPickupPoints.clear();
                    routeToPickupPoints.addAll(routePoints);
                    nearbyTrips.clear();
                    tripsAdapter.notifyDataSetChanged();
                    renderAcceptedTripPanel();
                    updateMapMarkers();
                    startDriverLocationUpdates();
                    startAcceptedTripPolling();

                    if (finalRouteError != null) {
                        Toast.makeText(
                                requireContext(),
                                "Đã nhận chuyến nhưng chưa vẽ được tuyến đến điểm đón",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error accepting trip", e);
                mainHandler.post(() -> {
                    showLoading(false);
                    Toast.makeText(requireContext(), "Không thể nhận chuyến: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void loadActiveTripOrNearby() {
        if (dataExecutor == null || dataExecutor.isShutdown()) {
            return;
        }

        showLoading(true);
        Context appContext = requireContext().getApplicationContext();
        dataExecutor.execute(() -> {
            try {
                Trip active = TripService.fetchActiveTrip(appContext);
                if (active == null
                        || (!"Accepted".equalsIgnoreCase(active.getStatus())
                        && !"InProgress".equalsIgnoreCase(active.getStatus()))) {
                    mainHandler.post(() -> {
                        if (isAdded()) {
                            showLoading(false);
                            refreshNearbyTrips();
                        }
                    });
                    return;
                }

                LatLng freshDriverLocation = resolveCurrentDriverLocation(appContext);
                if (freshDriverLocation != null) {
                    try {
                        active = TripService.updateDriverLocation(
                                appContext,
                                active.getId(),
                                freshDriverLocation.latitude,
                                freshDriverLocation.longitude
                        );
                    } catch (Exception exception) {
                        Log.w(TAG, "Unable to publish resumed driver location", exception);
                    }
                }

                boolean routeToDestination = "InProgress".equalsIgnoreCase(active.getStatus());
                LatLng routeStart = freshDriverLocation != null
                        ? freshDriverLocation
                        : getTripDriverLatLng(active);
                List<LatLng> routePoints = new ArrayList<>();

                try {
                    RoutePreview routePreview = RouteService.fetchRoutePreview(
                            appContext,
                            routeToDestination
                                    ? buildPickupRouteQuery(active)
                                    : (routeStart == null ? "" : formatLatLng(routeStart)),
                            routeToDestination
                                    ? buildDestinationRouteQuery(active)
                                    : buildPickupRouteQuery(active)
                    );
                    routePoints.addAll(routePreview.routePoints);
                } catch (Exception exception) {
                    Log.w(TAG, "Unable to restore active trip route", exception);
                }

                Trip finalActive = active;
                LatLng finalDriverLocation = routeStart;
                boolean finalRouteToDestination = routeToDestination;
                mainHandler.post(() -> {
                    if (!isAdded()) {
                        return;
                    }

                    showLoading(false);
                    selectedTrip = finalActive;
                    driverLocation = finalDriverLocation;
                    navigatingToPickup = true;
                    routingToDestination = finalRouteToDestination;
                    routeToPickupPoints.clear();
                    routeToPickupPoints.addAll(routePoints);
                    nearbyTrips.clear();
                    tripsAdapter.notifyDataSetChanged();
                    renderAcceptedTripPanel();
                    updateMapMarkers();
                    startDriverLocationUpdates();
                    startAcceptedTripPolling();
                });
            } catch (Exception exception) {
                Log.w(TAG, "Unable to load active driver trip", exception);
                mainHandler.post(() -> {
                    if (isAdded()) {
                        showLoading(false);
                        refreshNearbyTrips();
                    }
                });
            }
        });
    }

    private void showCancelAcceptedTripDialog() {
        if (selectedTrip == null) {
            return;
        }

        if (isTripInProgress()) {
            Toast.makeText(requireContext(), "Chuyến đi đã bắt đầu nên không thể hủy", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Hủy chuyến")
                .setMessage("Bạn có chắc muốn hủy chuyến đã nhận không?")
                .setNegativeButton("Ở lại", null)
                .setPositiveButton("Hủy chuyến", (dialog, which) -> cancelAcceptedTrip())
                .show();
    }

    private void cancelAcceptedTrip() {
        if (selectedTrip == null || dataExecutor == null || dataExecutor.isShutdown()) {
            return;
        }

        if (pendingDriverLocationUpdate != null) {
            mainHandler.removeCallbacks(pendingDriverLocationUpdate);
            pendingDriverLocationUpdate = null;
        }
        if (pendingAcceptedTripPoll != null) {
            mainHandler.removeCallbacks(pendingAcceptedTripPoll);
            pendingAcceptedTripPoll = null;
        }

        btnAcceptTrip.setEnabled(false);
        btnCancelTrip.setEnabled(false);
        showLoading(true);
        Context appContext = requireContext().getApplicationContext();
        String tripId = selectedTrip.getId();

        dataExecutor.execute(() -> {
            try {
                TripService.cancelTrip(appContext, tripId);
                mainHandler.post(() -> {
                    if (!isAdded()) {
                        return;
                    }

                    showLoading(false);
                    Toast.makeText(requireContext(), "Đã hủy chuyến", Toast.LENGTH_SHORT).show();
                    clearActiveTripAndRefresh();
                });
            } catch (Exception exception) {
                Log.w(TAG, "Unable to cancel accepted trip", exception);
                mainHandler.post(() -> {
                    if (!isAdded()) {
                        return;
                    }

                    showLoading(false);
                    renderAcceptedTripPanel();
                    updateMapMarkers();
                    startDriverLocationUpdates();
                    startAcceptedTripPolling();
                    Toast.makeText(requireContext(), "Không thể hủy chuyến: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void confirmDriverPickup() {
        if (selectedTrip == null || dataExecutor == null || dataExecutor.isShutdown()) {
            return;
        }

        LatLng pickupLocation = getTripPickupLatLng(selectedTrip);
        if (pickupLocation == null) {
            Toast.makeText(requireContext(), "Thiếu vị trí điểm đón của chuyến", Toast.LENGTH_SHORT).show();
            return;
        }

        btnAcceptTrip.setEnabled(false);
        showLoading(true);
        Context appContext = requireContext().getApplicationContext();
        String tripId = selectedTrip.getId();

        dataExecutor.execute(() -> {
            try {
                LatLng currentLocation = resolveCurrentDriverLocation(appContext);
                if (currentLocation == null) {
                    throw new IOException("Không lấy được vị trí hiện tại của tài xế");
                }

                double distanceMeters = calculateDistanceMeters(currentLocation, pickupLocation);
                if (distanceMeters > PICKUP_CONFIRM_RADIUS_METERS) {
                    throw new IOException("Bạn cần cách điểm đón tối đa 50m để xác nhận");
                }

                Trip updatedTrip = TripService.confirmPickup(
                        appContext,
                        tripId,
                        currentLocation.latitude,
                        currentLocation.longitude
                );

                mainHandler.post(() -> {
                    if (!isAdded()) {
                        return;
                    }

                    showLoading(false);
                    driverLocation = currentLocation;
                    selectedTrip = updatedTrip;
                    if ("InProgress".equalsIgnoreCase(updatedTrip.getStatus())) {
                        startRouteToDestination(updatedTrip);
                    } else {
                        Toast.makeText(requireContext(), "Đã xác nhận đón khách", Toast.LENGTH_SHORT).show();
                        renderAcceptedTripPanel();
                        updateMapMarkers();
                        startAcceptedTripPolling();
                    }
                });
            } catch (Exception exception) {
                Log.w(TAG, "Unable to confirm driver pickup", exception);
                mainHandler.post(() -> {
                    if (!isAdded()) {
                        return;
                    }

                    showLoading(false);
                    renderAcceptedTripPanel();
                    Toast.makeText(requireContext(), "Không thể xác nhận: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void completeDriverTrip() {
        if (selectedTrip == null || dataExecutor == null || dataExecutor.isShutdown()) {
            return;
        }

        LatLng destinationLocation = getTripDestinationLatLng(selectedTrip);
        if (destinationLocation == null) {
            Toast.makeText(requireContext(), "Thiếu vị trí điểm đến của chuyến", Toast.LENGTH_SHORT).show();
            return;
        }

        btnAcceptTrip.setEnabled(false);
        showLoading(true);
        Context appContext = requireContext().getApplicationContext();
        String tripId = selectedTrip.getId();

        dataExecutor.execute(() -> {
            try {
                LatLng currentLocation = resolveCurrentDriverLocation(appContext);
                if (currentLocation == null) {
                    throw new IOException("Không lấy được vị trí hiện tại của tài xế");
                }

                double distanceMeters = calculateDistanceMeters(currentLocation, destinationLocation);
                if (distanceMeters > DESTINATION_COMPLETE_RADIUS_METERS) {
                    throw new IOException("Bạn cần cách điểm đến tối đa 50m để hoàn thành chuyến");
                }

                Trip completedTrip = TripService.completeTrip(
                        appContext,
                        tripId,
                        currentLocation.latitude,
                        currentLocation.longitude
                );

                mainHandler.post(() -> {
                    if (!isAdded()) {
                        return;
                    }

                    showLoading(false);
                    driverLocation = currentLocation;
                    selectedTrip = completedTrip;
                    Toast.makeText(requireContext(), "Đã hoàn thành chuyến", Toast.LENGTH_SHORT).show();
                    clearActiveTripAndRefresh();
                });
            } catch (Exception exception) {
                Log.w(TAG, "Unable to complete trip", exception);
                mainHandler.post(() -> {
                    if (!isAdded()) {
                        return;
                    }

                    showLoading(false);
                    renderAcceptedTripPanel();
                    Toast.makeText(requireContext(), "Không thể hoàn thành chuyến: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void startDriverLocationUpdates() {
        if (!navigatingToPickup) {
            return;
        }

        if (pendingDriverLocationUpdate != null) {
            mainHandler.removeCallbacks(pendingDriverLocationUpdate);
        }

        pendingDriverLocationUpdate = this::refreshDriverLocationForPickup;
        mainHandler.postDelayed(pendingDriverLocationUpdate, DRIVER_LOCATION_UPDATE_INTERVAL_MS);
    }

    private void refreshDriverLocationForPickup() {
        pendingDriverLocationUpdate = null;
        if (!navigatingToPickup || dataExecutor == null || dataExecutor.isShutdown()) {
            return;
        }

        Context appContext = requireContext().getApplicationContext();
        dataExecutor.execute(() -> {
            LatLng currentLocation = resolveCurrentDriverLocation(appContext);
            Trip updatedTrip = null;
            if (currentLocation != null && selectedTrip != null) {
                try {
                    updatedTrip = TripService.updateDriverLocation(
                            appContext,
                            selectedTrip.getId(),
                            currentLocation.latitude,
                            currentLocation.longitude
                    );
                } catch (Exception exception) {
                    Log.w(TAG, "Unable to publish driver location", exception);
                }
            }
            Trip finalUpdatedTrip = updatedTrip;
            mainHandler.post(() -> {
                if (!isAdded()) {
                    return;
                }
                if (currentLocation == null) {
                    startDriverLocationUpdates();
                    return;
                }

                driverLocation = currentLocation;
                if (finalUpdatedTrip != null) {
                    selectedTrip = finalUpdatedTrip;
                }
                renderAcceptedTripPanel();
                updateMapMarkers();
                startDriverLocationUpdates();
            });
        });
    }

    private void startAcceptedTripPolling() {
        if (selectedTrip == null || dataExecutor == null || dataExecutor.isShutdown()) {
            return;
        }

        if (pendingAcceptedTripPoll != null) {
            mainHandler.removeCallbacks(pendingAcceptedTripPoll);
        }

        pendingAcceptedTripPoll = this::pollAcceptedTripStatus;
        mainHandler.postDelayed(pendingAcceptedTripPoll, ACCEPTED_TRIP_POLL_INTERVAL_MS);
    }

    private void pollAcceptedTripStatus() {
        pendingAcceptedTripPoll = null;
        if (selectedTrip == null || dataExecutor == null || dataExecutor.isShutdown()) {
            return;
        }

        Context appContext = requireContext().getApplicationContext();
        String tripId = selectedTrip.getId();
        dataExecutor.execute(() -> {
            try {
                Trip latestTrip = TripService.fetchTrip(appContext, tripId);
                mainHandler.post(() -> handleAcceptedTripUpdate(latestTrip));
            } catch (Exception exception) {
                Log.w(TAG, "Unable to poll accepted trip", exception);
                mainHandler.post(() -> {
                    if (isAdded()) {
                        startAcceptedTripPolling();
                    }
                });
            }
        });
    }

    private void handleAcceptedTripUpdate(@NonNull Trip latestTrip) {
        if (!isAdded()) {
            return;
        }

        selectedTrip = latestTrip;
        if ("Completed".equalsIgnoreCase(latestTrip.getStatus())
                || "Cancelled".equalsIgnoreCase(latestTrip.getStatus())) {
            String message = "Cancelled".equalsIgnoreCase(latestTrip.getStatus())
                    ? "Chuyến đi đã bị hủy"
                    : "Chuyến đi đã kết thúc";
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            clearActiveTripAndRefresh();
            return;
        }

        if ("InProgress".equalsIgnoreCase(latestTrip.getStatus())) {
            if (routingToDestination) {
                renderAcceptedTripPanel();
                updateMapMarkers();
                startAcceptedTripPolling();
                return;
            }
            startRouteToDestination(latestTrip);
            return;
        }

        renderAcceptedTripPanel();
        updateMapMarkers();
        startAcceptedTripPolling();
    }

    private void startRouteToDestination(@NonNull Trip trip) {
        if (dataExecutor == null || dataExecutor.isShutdown()) {
            return;
        }

        showLoading(true);
        Context appContext = requireContext().getApplicationContext();
        dataExecutor.execute(() -> {
            try {
                RoutePreview routePreview = RouteService.fetchRoutePreview(
                        appContext,
                        buildPickupRouteQuery(trip),
                        buildDestinationRouteQuery(trip)
                );
                List<LatLng> routePoints = new ArrayList<>(routePreview.routePoints);

                mainHandler.post(() -> {
                    if (!isAdded()) {
                        return;
                    }

                    showLoading(false);
                    selectedTrip = trip;
                    navigatingToPickup = true;
                    routingToDestination = true;
                    routeToPickupPoints.clear();
                    routeToPickupPoints.addAll(routePoints);
                    renderAcceptedTripPanel();
                    updateMapMarkers();
                    startDriverLocationUpdates();
                    startAcceptedTripPolling();
                    Toast.makeText(requireContext(), "Chuyến đi đã bắt đầu", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception exception) {
                Log.w(TAG, "Unable to draw route to destination", exception);
                mainHandler.post(() -> {
                    if (!isAdded()) {
                        return;
                    }

                    showLoading(false);
                    selectedTrip = trip;
                    navigatingToPickup = true;
                    routingToDestination = true;
                    routeToPickupPoints.clear();
                    renderAcceptedTripPanel();
                    updateMapMarkers();
                    startDriverLocationUpdates();
                    startAcceptedTripPolling();
                    Toast.makeText(requireContext(), "Chuyến đã bắt đầu nhưng chưa vẽ được tuyến đến điểm đến", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Nullable
    private LatLng resolveCurrentDriverLocation(@NonNull Context context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return null;
        }

        try {
            Location location = Tasks.await(
                    LocationServices.getFusedLocationProviderClient(context)
                            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null),
                    LOCATION_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS
            );

            if (location == null) {
                location = Tasks.await(
                        LocationServices.getFusedLocationProviderClient(context).getLastLocation(),
                        3L,
                        TimeUnit.SECONDS
                );
            }

            if (location == null) {
                return null;
            }

            return new LatLng(location.getLatitude(), location.getLongitude());
        } catch (SecurityException exception) {
            return null;
        } catch (ExecutionException | TimeoutException exception) {
            return null;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    @NonNull
    private String buildPickupRouteQuery(@NonNull Trip trip) {
        LatLng pickupLocation = getTripPickupLatLng(trip);
        if (pickupLocation != null) {
            return formatLatLng(pickupLocation);
        }

        return TextUtils.isEmpty(trip.getPickupAddress()) ? "" : trip.getPickupAddress();
    }

    @NonNull
    private String buildDestinationRouteQuery(@NonNull Trip trip) {
        LatLng destinationLocation = getTripDestinationLatLng(trip);
        if (destinationLocation != null) {
            return formatLatLng(destinationLocation);
        }

        return TextUtils.isEmpty(trip.getDestinationAddress()) ? "" : trip.getDestinationAddress();
    }

    @NonNull
    private String formatLatLng(@NonNull LatLng latLng) {
        return String.format(Locale.US, "%.6f,%.6f", latLng.latitude, latLng.longitude);
    }

    @Nullable
    private LatLng getTripPickupLatLng(@NonNull Trip trip) {
        double lat = trip.getPickupLat();
        double lng = trip.getPickupLng();
        if (!isValidCoordinate(lat, lng)) {
            return null;
        }

        return new LatLng(lat, lng);
    }

    @Nullable
    private LatLng getTripDestinationLatLng(@NonNull Trip trip) {
        double lat = trip.getDestinationLat();
        double lng = trip.getDestinationLng();
        if (!isValidCoordinate(lat, lng)) {
            return null;
        }

        return new LatLng(lat, lng);
    }

    @Nullable
    private LatLng getTripDriverLatLng(@NonNull Trip trip) {
        double lat = trip.getDriverCurrentLat();
        double lng = trip.getDriverCurrentLng();
        if (!isValidCoordinate(lat, lng)) {
            return null;
        }

        return new LatLng(lat, lng);
    }

    private boolean isValidCoordinate(double lat, double lng) {
        return lat >= -90d && lat <= 90d
                && lng >= -180d && lng <= 180d
                && !(Math.abs(lat) < 0.000001d && Math.abs(lng) < 0.000001d);
    }

    private double calculateDistanceMeters(@NonNull LatLng start, @NonNull LatLng end) {
        double earthRadiusMeters = 6371000d;
        double dLat = Math.toRadians(end.latitude - start.latitude);
        double dLng = Math.toRadians(end.longitude - start.longitude);
        double startLat = Math.toRadians(start.latitude);
        double endLat = Math.toRadians(end.latitude);
        double a = Math.sin(dLat / 2d) * Math.sin(dLat / 2d)
                + Math.cos(startLat) * Math.cos(endLat)
                * Math.sin(dLng / 2d) * Math.sin(dLng / 2d);
        double c = 2d * Math.atan2(Math.sqrt(a), Math.sqrt(1d - a));
        return earthRadiusMeters * c;
    }

    @NonNull
    private String formatMeters(double distanceMeters) {
        if (distanceMeters >= 1000d) {
            return String.format(Locale.getDefault(), "%.1f km", distanceMeters / 1000d);
        }

        return Math.max(1, Math.round(distanceMeters)) + " m";
    }

    private void renderRouteToPickup() {
        if (mMap == null || selectedTrip == null) {
            return;
        }

        LatLng pickupLocation = getTripPickupLatLng(selectedTrip);
        if (driverLocation != null) {
            mMap.addMarker(new MarkerOptions()
                    .position(driverLocation)
                    .title("Vị trí tài xế")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        }

        if (pickupLocation != null) {
            addPickupMarker(pickupLocation, selectedTrip.getPickupAddress());
        }

        if (!routeToPickupPoints.isEmpty()) {
            mMap.addPolyline(new PolylineOptions()
                    .addAll(routeToPickupPoints)
                    .color(ContextCompat.getColor(requireContext(), R.color.primary))
                    .width(10f)
                    .geodesic(true));
        }

        fitRouteBounds(driverLocation, pickupLocation, routeToPickupPoints);
    }

    private void renderRouteToDestination() {
        if (mMap == null || selectedTrip == null) {
            return;
        }

        LatLng pickupLocation = getTripPickupLatLng(selectedTrip);
        LatLng destinationLocation = getTripDestinationLatLng(selectedTrip);

        if (driverLocation != null) {
            mMap.addMarker(new MarkerOptions()
                    .position(driverLocation)
                    .title("Vị trí tài xế")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        }

        if (pickupLocation != null) {
            addPickupMarker(pickupLocation, selectedTrip.getPickupAddress());
        }

        if (destinationLocation != null) {
            addDestinationMarker(destinationLocation, selectedTrip.getDestinationAddress());
        }

        if (!routeToPickupPoints.isEmpty()) {
            mMap.addPolyline(new PolylineOptions()
                    .addAll(routeToPickupPoints)
                    .color(ContextCompat.getColor(requireContext(), R.color.primary))
                    .width(10f)
                    .geodesic(true));
        }

        fitRouteBounds(driverLocation != null ? driverLocation : pickupLocation, destinationLocation, routeToPickupPoints);
    }

    private void addPickupMarker(@NonNull LatLng pickupLocation, @Nullable String pickupAddress) {
        String displayAddress = CurrentLocationResolver.getShareableLocationLabel(pickupAddress);
        String title = TextUtils.isEmpty(displayAddress)
                ? "Điểm đón"
                : "Điểm đón: " + displayAddress;

        mMap.addMarker(new MarkerOptions()
                .position(pickupLocation)
                .title(title)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
    }

    private void addDestinationMarker(@NonNull LatLng destinationLocation, @Nullable String destinationAddress) {
        String displayAddress = CurrentLocationResolver.getShareableLocationLabel(destinationAddress);
        String title = TextUtils.isEmpty(displayAddress)
                ? "Điểm đến"
                : "Điểm đến: " + displayAddress;

        mMap.addMarker(new MarkerOptions()
                .position(destinationLocation)
                .title(title)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
    }

    private void fitRouteBounds(@Nullable LatLng start,
                                @Nullable LatLng end,
                                @NonNull List<LatLng> routePoints) {
        LatLngBounds.Builder builder = LatLngBounds.builder();
        int includedPoints = 0;

        if (start != null) {
            builder.include(start);
            includedPoints++;
        }
        if (end != null) {
            builder.include(end);
            includedPoints++;
        }
        for (LatLng routePoint : routePoints) {
            if (routePoint != null) {
                builder.include(routePoint);
                includedPoints++;
            }
        }

        if (includedPoints >= 2) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), ROUTE_PADDING_PX));
        } else if (includedPoints == 1) {
            LatLng focus = end != null ? end : start;
            if (focus != null) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(focus, CAMERA_ZOOM));
            }
        }
    }

    private void showLoading(boolean show) {
        loadingProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
