package vnuhcm.hcmus.movify.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import vnuhcm.hcmus.movify.R;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private SupportMapFragment mapFragment;

    public MapFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Tìm SupportMapFragment từ layout
        mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Thiết lập vị trí mặc định
        LatLng hcmc = new LatLng(10.8231, 106.6297);
        mMap.addMarker(new MarkerOptions().position(hcmc).title("Thành phố Hồ Chí Minh"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(hcmc, 12f));

        // Bật My Location button nếu có permission
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                if (requireActivity().checkSelfPermission(
                        android.Manifest.permission.ACCESS_FINE_LOCATION)
                        == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                }
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        // Thiết lập map click listener
        mMap.setOnMapClickListener(latLng -> {
            // Thêm marker mới khi click trên map
            mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("Vị trí đã chọn"));
        });
    }
}
