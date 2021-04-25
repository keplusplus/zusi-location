package de.esudo.zusilocation;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.LocationComponentOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;

import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, PermissionsListener {

    public static final String TAG = "!LOG-ZusiLocation";
    private MapView mMapView;
    public MapboxMap mMap; // private MapboxMap mMap;
    private PermissionsManager permissionsManager;
    private boolean active;
    private ZusiConnection zusiConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));
        setContentView(R.layout.activity_main);

        active = false;

        mMapView = findViewById(R.id.map_view);
        mMapView.onCreate(savedInstanceState);
        mMapView.getMapAsync(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.toggle_status) {
            if(active) {
                deactivate(item);
            } else {
                activate(item);
            }
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        MainActivity.this.mMap = mapboxMap;
        mapboxMap.setStyle(Style.MAPBOX_STREETS, style -> {
            enableLocationComponent(style);
        });
    }

    @SuppressLint("MissingPermission")
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
        if(PermissionsManager.areLocationPermissionsGranted(this)) {
            LocationComponentOptions lco = LocationComponentOptions.builder(this).pulseEnabled(true).build();
            LocationComponent lc = mMap.getLocationComponent();
            lc.activateLocationComponent(LocationComponentActivationOptions.builder(this, loadedMapStyle).locationComponentOptions(lco).build());
            lc.setLocationComponentEnabled(true);

            lc.setCameraMode(CameraMode.TRACKING);
            lc.setRenderMode(RenderMode.NORMAL);
            Location l = lc.getLastKnownLocation();
            mMap.setCameraPosition(new CameraPosition.Builder().target(new LatLng(l.getLatitude(), l.getLongitude())).zoom(8).build());
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, "Ich brauch Standortzugriff...", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if(granted) {
            mMap.getStyle(new Style.OnStyleLoaded() {
                @Override
                public void onStyleLoaded(@NonNull Style style) {
                    enableLocationComponent(style);
                }
            });
        } else {
            Toast.makeText(this, ":(", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mMapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mMapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    public void activate(@NonNull  MenuItem item) {
        active = true;
        readConnectionParamsAndGo(item);
    }

    public void deactivate(@NonNull MenuItem item) {
        active = false;
        item.setIcon(R.drawable.outline_play_arrow_24);
        item.setTitle("Start mock location");
        if(zusiConnection != null) zusiConnection.cancel(true);
    }

    private ZusiConnection.ConnectionParams readConnectionParamsAndGo(MenuItem item) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        final ZusiConnection.ConnectionParams params = new ZusiConnection.ConnectionParams(
                sharedPreferences.getString("connection_host", ""),
                sharedPreferences.getInt("connection_port", 1436),
                generateId());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Connect");
        View v = getLayoutInflater().inflate(R.layout.connection_params_layout, null);
        builder.setView(v);

        final TextInputEditText hostInput = v.findViewById(R.id.host_input);
        hostInput.setText(params.getHost());
        final TextInputEditText portInput = v.findViewById(R.id.port_input);
        portInput.setText(String.valueOf(params.getPort()));

        builder.setPositiveButton("Connect", (dialogInterface, i) -> {
            params.setHost(hostInput.getText().toString());
            sharedPreferences.edit().putString("connection_host", params.getHost()).putInt("connection_port", params.getPort()).apply();

            zusiConnection = new ZusiConnection(MainActivity.this);
            zusiConnection.execute(params);

            item.setIcon(R.drawable.outline_stop_24);
            item.setTitle("Stop mock location");

            dialogInterface.dismiss();
        });

        builder.setNegativeButton("Cancel", ((dialogInterface, i) -> dialogInterface.dismiss()));

        builder.show();

        return params;
    }

    public char[] generateId() {
        char[] hexArray = "0123456789abcdef".toCharArray();
        byte[] id = new byte[4];
        new Random().nextBytes(id);
        char[] hexChars = new char[id.length * 2];
        for(int i = 0; i < id.length; i++) {
            int v = id[i] & 0xFF;
            hexChars[i * 2] = hexArray[v >>> 4];
            hexChars[i * 2 + 1] = hexArray[v & 0x0F];
        }
        return hexChars;
    }
}