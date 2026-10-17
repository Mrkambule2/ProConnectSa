package com.example.proconnectsa;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LatLng selectedLatLng;
    private boolean isPickMode = false;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private EditText editSearch;
    private ImageButton btnSearch;
    private MaterialButton btnConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        isPickMode = getIntent().getBooleanExtra("pick_mode", false);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        editSearch = findViewById(R.id.edit_search_location);
        btnSearch = findViewById(R.id.btn_search);
        btnConfirm = findViewById(R.id.btn_confirm_location);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        findViewById(R.id.fab_my_location).setOnClickListener(v -> getDeviceLocation());

        btnSearch.setOnClickListener(v -> searchLocation());
        editSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchLocation();
                return true;
            }
            return false;
        });

        if (isPickMode) {
            btnConfirm.setVisibility(View.VISIBLE);
            btnConfirm.setOnClickListener(v -> {
                if (selectedLatLng != null) {
                    returnResult(selectedLatLng);
                } else {
                    Toast.makeText(this, "Please select a location on the map", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false); // We use our own FAB

        LatLng initial = new LatLng(-26.2041, 28.0473); // Joburg
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initial, 12));

        if (isPickMode) {
            mMap.setOnMapClickListener(latLng -> {
                updateMarker(latLng);
            });
        }

        enableMyLocation();
    }

    private void updateMarker(LatLng latLng) {
        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(latLng).title("Selected Point"));
        selectedLatLng = latLng;
    }

    private void searchLocation() {
        String locationName = editSearch.getText().toString().trim();
        if (locationName.isEmpty()) return;

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addressList = geocoder.getFromLocationName(locationName, 1);
            if (addressList != null && !addressList.isEmpty()) {
                Address address = addressList.get(0);
                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                updateMarker(latLng);
            } else {
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Search error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        if (mMap != null) {
            mMap.setMyLocationEnabled(true);
        }
    }

    private void getDeviceLocation() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                    if (location != null) {
                        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                        if (isPickMode) {
                            updateMarker(latLng);
                        }
                    } else {
                        Toast.makeText(this, "Unable to get current location. Is GPS on?", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                enableMyLocation();
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void returnResult(LatLng latLng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        String addressText = String.format("%.4f, %.4f", latLng.latitude, latLng.longitude);
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address addr = addresses.get(0);
                StringBuilder sb = new StringBuilder();
                if (addr.getMaxAddressLineIndex() >= 0) {
                    sb.append(addr.getAddressLine(0));
                } else {
                    sb.append(addr.getLocality()).append(", ").append(addr.getAdminArea());
                }
                addressText = sb.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Intent data = new Intent();
        data.putExtra("address", addressText);
        data.putExtra("latitude", latLng.latitude);
        data.putExtra("longitude", latLng.longitude);
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            }
        }
    }
}
