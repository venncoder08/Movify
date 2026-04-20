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
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vnuhcm.hcmus.movify.R;
import vnuhcm.hcmus.movify.utils.LocationData;
import vnuhcm.hcmus.movify.utils.PlacesAutocompleteHelper;
import vnuhcm.hcmus.movify.models.PlaceDetails;
import vnuhcm.hcmus.movify.models.RoutePreview;
import vnuhcm.hcmus.movify.services.PlaceService;
import vnuhcm.hcmus.movify.services.RouteService;

public class MapBookingFragment extends Fragment implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final String TAG = "MapBookingFragment";
    private static final long MAP_UPDATE_DEBOUNCE_MS = 600L;
    private static final int ROUTE_WIDTH_PX = 10;
    private static final int CAMERA_PADDING_PX = 120;
    private static final int ROUTE_COLOR = 0xFFD32F2F;
    private static final LatLng HCMC = new LatLng(10.8231, 106.6297);

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private GoogleMap mMap;
    private SupportMapFragment mapFragment;
    private LatLng selectedPickupLocation;
    private LatLng selectedDestinationLocation;
    private AutoCompleteTextView edtMapPickup;
    private AutoCompleteTextView edtMapDestination;
    private Button btnConfirmLocation;
    private ExecutorService mapExecutor;
    private Runnable pendingMapUpdate;
    private int activeMapRequestId = 0;

    public MapBookingFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map_booking, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        edtMapPickup = view.findViewById(R.id.edtMapPickup);
        edtMapDestination = view.findViewById(R.id.edtMapDestination);
        btnConfirmLocation = view.findViewById(R.id.btnConfirmLocation);
        mapExecutor = Executors.newSingleThreadExecutor();

        new PlacesAutocompleteHelper(requireContext(), edtMapPickup);
        new PlacesAutocompleteHelper(requireContext(), edtMapDestination);

        requestLocationPermission();
        attachMapUpdateWatcher(edtMapPickup);
        attachMapUpdateWatcher(edtMapDestination);
        btnConfirmLocation.setOnClickListener(v -> confirmAndReturn());

        mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.mapBooking);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mainHandler.removeCallbacksAndMessages(null);
        if (mapExecutor != null) {
            mapExecutor.shutdownNow();
            mapExecutor = null;
        }

        mMap = null;
        mapFragment = null;
        pendingMapUpdate = null;
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
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(HCMC, 13f));
        enableMyLocation();
        scheduleMapUpdate();
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

    private void attachMapUpdateWatcher(AutoCompleteTextView input) {
        input.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                scheduleMapUpdate();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });
    }

    private void scheduleMapUpdate() {
        if (mMap == null || mapExecutor == null || mapExecutor.isShutdown()) {
            return;
        }

        if (pendingMapUpdate != null) {
            mainHandler.removeCallbacks(pendingMapUpdate);
        }

        final int requestId = ++activeMapRequestId;
        pendingMapUpdate = () -> updateMapState(requestId);
        mainHandler.postDelayed(pendingMapUpdate, MAP_UPDATE_DEBOUNCE_MS);
    }

    private void updateMapState(int requestId) {
        if (mapExecutor == null || mapExecutor.isShutdown()) {
            return;
        }

        final Context appContext = requireContext().getApplicationContext();
        final String pickupText = edtMapPickup.getText().toString().trim();
        final String destinationText = edtMapDestination.getText().toString().trim();

        mapExecutor.execute(() -> {
            PlaceDetails pickupDetails = null;
            PlaceDetails destinationDetails = null;
            List<LatLng> routePoints = new ArrayList<>();
            String errorMessage = null;

            try {
                if (!pickupText.isEmpty() && !destinationText.isEmpty()) {
                    RoutePreview routePreview =
                            RouteService.fetchRoutePreview(appContext, pickupText, destinationText);
                    pickupDetails = routePreview.pickup;
                    destinationDetails = routePreview.destination;
                    routePoints = routePreview.routePoints;
                } else {
                    if (!pickupText.isEmpty()) {
                        pickupDetails = PlaceService.fetchPlaceDetails(appContext, pickupText);
                    }
                    if (!destinationText.isEmpty()) {
                        destinationDetails = PlaceService.fetchPlaceDetails(appContext, destinationText);
                    }
                }
            } catch (Exception exception) {
                Log.e(TAG, "Unable to fetch map preview from backend", exception);
                errorMessage = exception.getMessage();
            }

            PlaceDetails finalPickupDetails = pickupDetails;
            PlaceDetails finalDestinationDetails = destinationDetails;
            List<LatLng> finalRoutePoints = routePoints;
            String finalErrorMessage = errorMessage;

            mainHandler.post(() -> renderMapState(
                    requestId,
                    finalPickupDetails,
                    finalDestinationDetails,
                    finalRoutePoints,
                    finalErrorMessage
            ));
        });
    }

    private void renderMapState(int requestId,
                                @Nullable PlaceDetails pickupDetails,
                                @Nullable PlaceDetails destinationDetails,
                                @NonNull List<LatLng> routePoints,
                                @Nullable String errorMessage) {
        if (!isAdded() || mMap == null || requestId != activeMapRequestId) {
            return;
        }

        selectedPickupLocation = pickupDetails != null ? pickupDetails.latLng : null;
        selectedDestinationLocation = destinationDetails != null ? destinationDetails.latLng : null;

        mMap.clear();

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        boolean hasBounds = false;

        if (pickupDetails != null) {
            mMap.addMarker(new MarkerOptions()
                    .position(pickupDetails.latLng)
                    .title("Pickup: " + pickupDetails.label)
                    .snippet("Pickup location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            boundsBuilder.include(pickupDetails.latLng);
            hasBounds = true;
        }

        if (destinationDetails != null) {
            mMap.addMarker(new MarkerOptions()
                    .position(destinationDetails.latLng)
                    .title("Destination: " + destinationDetails.label)
                    .snippet("Destination location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            boundsBuilder.include(destinationDetails.latLng);
            hasBounds = true;
        }

        if (!routePoints.isEmpty()) {
            mMap.addPolyline(new PolylineOptions()
                    .addAll(routePoints)
                    .color(ROUTE_COLOR)
                    .width(ROUTE_WIDTH_PX)
                    .geodesic(true)
                    .zIndex(100f));

            for (LatLng point : routePoints) {
                boundsBuilder.include(point);
            }
            hasBounds = true;
        } else if (errorMessage != null && !errorMessage.isBlank()) {
            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show();
            Log.w(TAG, "Route was not drawn: " + errorMessage);
        }

        if (!hasBounds) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(HCMC, 13f));
            return;
        }

        try {
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), CAMERA_PADDING_PX));
        } catch (IllegalStateException exception) {
            LatLng focusPoint = pickupDetails != null
                    ? pickupDetails.latLng
                    : destinationDetails != null ? destinationDetails.latLng : HCMC;
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(focusPoint, 14f));
        }
    }

    private void confirmAndReturn() {
        String pickupText = edtMapPickup.getText().toString().trim();
        String destinationText = edtMapDestination.getText().toString().trim();

        if (pickupText.isEmpty()) {
            Toast.makeText(requireContext(), "Nhap diem don", Toast.LENGTH_SHORT).show();
            return;
        }

        if (destinationText.isEmpty()) {
            Toast.makeText(requireContext(), "Nhap diem den", Toast.LENGTH_SHORT).show();
            return;
        }

        LocationData.setPickupLocation(pickupText);
        LocationData.setDestinationLocation(destinationText);
        getParentFragmentManager().popBackStack();
    }

    public LatLng getSelectedPickupLocation() {
        return selectedPickupLocation;
    }

    public LatLng getSelectedDestinationLocation() {
        return selectedDestinationLocation;
    }
}
