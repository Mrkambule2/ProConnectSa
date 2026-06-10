package com.example.proconnectsa;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LatLng selectedLatLng;
    private boolean isPickMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        isPickMode = getIntent().getBooleanExtra("pick_mode", false);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        if (isPickMode) {
            Button btnConfirm = new Button(this);
            btnConfirm.setText("Confirm Location");
            addContentView(btnConfirm, new android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
            
            btnConfirm.setOnClickListener(v -> {
                if (selectedLatLng != null) {
                    Intent data = new Intent();
                    data.putExtra("address", String.format("%.4f, %.4f", selectedLatLng.latitude, selectedLatLng.longitude));
                    setResult(RESULT_OK, data);
                    finish();
                } else {
                    Toast.makeText(this, "Please tap on map to select location", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng initial = new LatLng(-26.2041, 28.0473); // Joburg
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initial, 10));

        if (isPickMode) {
            mMap.setOnMapClickListener(latLng -> {
                mMap.clear();
                mMap.addMarker(new MarkerOptions().position(latLng).title("Selected Location"));
                selectedLatLng = latLng;
            });
        } else {
            mMap.addMarker(new MarkerOptions().position(initial).title("Job Lead"));
        }
    }
}
