package ch.coredump.watertemp;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.text.Layout;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mapbox.mapboxsdk.MapboxAccountManager;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.UiSettings;

import org.ocpsoft.prettytime.PrettyTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.coredump.watertemp.rest.ApiClient;
import ch.coredump.watertemp.rest.ApiService;
import ch.coredump.watertemp.rest.SensorMeasurements;
import ch.coredump.watertemp.rest.models.ApiError;
import ch.coredump.watertemp.rest.models.Measurement;
import ch.coredump.watertemp.rest.models.Sensor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MapActivity";

    private MapboxMap map;
    private MapView mapView;
    private ApiService apiService;
    private Map<Integer, SensorMeasurements> sensors = new HashMap<>();
    private Marker activeMarker;
    private BottomSheetBehavior bottomSheetBehavior;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Start mapbox account manager
        MapboxAccountManager.start(this.getApplicationContext(), getString(R.string.mapbox_access_token));

        // Initialize the layout
        setContentView(R.layout.activity_map);

        // Create map view
        mapView = (MapView) findViewById(R.id.map_view);
        assert mapView != null;
        mapView.onCreate(savedInstanceState);

        // Initialize map
        mapView.getMapAsync(this);

        // Get API client
        // TODO: Use singleton dependency injection using something like dagger 2
        final ApiClient apiClient = new ApiClient(getString(R.string.public_api_token));
        apiService = apiClient.getApiService();

        // Initialize bottom sheet behavior
        final NestedScrollView bottomSheetView = (NestedScrollView) findViewById(R.id.details_bottom_sheet);
        assert bottomSheetView != null;
        this.bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetView);

        // Set peek height as necessary
        RelativeLayout bottomSheetPeek = (RelativeLayout) findViewById(R.id.bottom_sheet_peek);
        assert bottomSheetPeek != null;
        this.bottomSheetBehavior.setPeekHeight(bottomSheetPeek.getHeight());

        // Add bottom sheet listener
        this.bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                // Bottom sheet state changed
                Log.d(TAG, "State changed to " + newState);
                // TODO: If it's gone, make sure to deselect all markers.
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // Called repeatedly while bottom sheet slides up
            }
        });
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        Log.d(TAG, "Map is ready");

        // Save map as attribute
        map = mapboxMap;

        // Disable interactions that might confuse the user
        UiSettings settings = map.getUiSettings();
        settings.setRotateGesturesEnabled(false);
        settings.setTiltGesturesEnabled(false);
        settings.setCompassEnabled(false);

        // Fetch sensors
        Call<List<Sensor>> sensorCall = apiService.listSensors();
        sensorCall.enqueue(onSensorsFetched());
    }

    private Callback<List<Sensor>> onSensorsFetched() {
        return new Callback<List<Sensor>>() {
            @Override
            public void onResponse(Call<List<Sensor>> call, Response<List<Sensor>> response) {
                if (response == null) {
                    Log.e(TAG, "Received null response from sensors endpoint");
                    return;
                }
                Log.i(TAG, "Sensor response done!");
                if (response.isSuccessful()) {
                    // Clear old sensor list
                    sensors.clear();

                    // Prepare list for sensor IDs
                    List<String> idList = new ArrayList<>();

                    // Extract sensor information
                    for (Sensor sensor : response.body()) {
                        sensors.put(sensor.getId(), new SensorMeasurements(sensor));
                        idList.add(String.valueOf(sensor.getId()));
                    }

                    // Fetch measurements
                    final String ids = Utils.join(",", idList);
                    Call<List<Measurement>> measurementCall = apiService.listMeasurements(ids, idList.size() * 5);
                    measurementCall.enqueue(onMeasurementsFetched());
                } else {
                    ApiError error = ApiClient.parseError(response);
                    Log.e(TAG, error.toString());
                    Utils.showError(MapActivity.this, "Could not fetch sensors.\n" +
                            error.getStatusCode() + ": " + error.getMessage());
                }
            }

            @Override
            public void onFailure(Call<List<Sensor>> call, Throwable t) {
                final String errmsg = "Fetching sensors failed: " + t.toString();
                Log.e(TAG, errmsg);
                Utils.showError(MapActivity.this, errmsg);
            }
        };
    }

    private Callback<List<Measurement>> onMeasurementsFetched() {
        return new Callback<List<Measurement>>() {
            @Override
            public void onResponse(Call<List<Measurement>> call, Response<List<Measurement>> response) {
                Log.i(TAG, "Measurement response done!");
                if (response != null) {
                    for (Measurement measurement : response.body()) {
                        Log.i(TAG, "Measurement: " + measurement.getTemperature());
                        sensors.get(measurement.getSensorId()).addMeasurement(measurement);
                    }
                    updateMarkers();
                }
            }

            @Override
            public void onFailure(Call<List<Measurement>> call, Throwable t) {
                final String errmsg = "Fetching measurements failed:" + t.toString();
                Log.e(TAG, errmsg);
                Utils.showError(MapActivity.this, errmsg);
            }
        };
    }

    private void updateMarkers() {
        // Process sensors
        final List<LatLng> locations = new ArrayList<>();
        for (SensorMeasurements sensorMeasurement : sensors.values()) {
            final Sensor sensor = sensorMeasurement.getSensor();
            final List<Measurement> measurements = sensorMeasurement.getMeasurements();
            Log.i(TAG, "Add sensor" + sensor.getCaption());

            // Sort measurements by ID
            Collections.sort(measurements, new Comparator<Measurement>() {
                @Override
                public int compare(Measurement lhs, Measurement rhs) {
                    final Integer leftId = lhs.getId();
                    final Integer rightId = rhs.getId();
                    return leftId.compareTo(rightId);
                }
            });

            // Create location object
            final float lat = sensor.getLocation().getLatitude();
            final float lng = sensor.getLocation().getLongitude();
            final LatLng location = new LatLng(lat, lng);

            // Build caption
            final StringBuilder captionBuilder = new StringBuilder();
            if (measurements.size() > 0) {
                final Measurement measurement = measurements.get(measurements.size() - 1);
                final PrettyTime pt = new PrettyTime();
                captionBuilder.append(measurement.getTemperature());
                captionBuilder.append("°C (");
                captionBuilder.append(pt.format(measurement.getCreatedAt()));
                captionBuilder.append(")");
            } else {
                captionBuilder.append("No current measurement");
            }

            // Initialize icons
            Context context = getApplicationContext();
            IconFactory iconFactory = IconFactory.getInstance(context);
            // TODO: Proper tinting of blue marker like primary dark color
            Drawable defaultIconDrawable = ContextCompat.getDrawable(context, R.drawable.blue_marker);
            // Default marker uses our accent color
            Drawable activeIconDrawable = ContextCompat.getDrawable(context, R.drawable.default_marker);
            final Icon defaultIcon = iconFactory.fromDrawable(defaultIconDrawable);
            final Icon activeIcon = iconFactory.fromDrawable(activeIconDrawable);

            // Add the marker to the map
            map.addMarker(
                    new MarkerOptions()
                            .position(new LatLng(lat, lng))
                            .title(sensor.getDeviceName())
                            .snippet(captionBuilder.toString())
                            .icon(defaultIcon)
            );

            map.setOnMarkerClickListener(new MapboxMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(@NonNull Marker marker) {
                    Log.d(TAG, "Marker ID: " + marker.getId());

                    // Update active marker icon
                    if (MapActivity.this.activeMarker != null) {
                        MapActivity.this.activeMarker.setIcon(defaultIcon);
                    }
                    marker.setIcon(activeIcon);
                    MapActivity.this.activeMarker = marker;

                    // Update detail pane
                    final TextView title = (TextView) findViewById(R.id.details_title);
                    assert title != null;
                    final TextView measurement = (TextView) findViewById(R.id.details_measurement);
                    assert measurement != null;
                    title.setText(marker.getTitle());
                    measurement.setText(marker.getSnippet());

                    // Show the details pane
                    if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    }

                    return true;
                }
            });
            map.setOnMapClickListener(new MapboxMap.OnMapClickListener() {
                @Override
                public void onMapClick(@NonNull LatLng point) {
                    Log.d(TAG,  "Clicked on map");

                    if (MapActivity.this.activeMarker == null) {
                        return;
                    }

                    // No more active marker
                    MapActivity.this.activeMarker.setIcon(defaultIcon);
                    MapActivity.this.activeMarker = null;

                    // Hide the details pane
                    if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    }
                }
            });

            // Store location
            locations.add(location);
        }

        // Change zoom to include all markers
        if (locations.size() == 1) {
            final LatLng location = locations.get(0);
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 13));
        } else if (sensors.size() > 1) {
            LatLngBounds.Builder boundingBoxBuilder = new LatLngBounds.Builder();
            for (final LatLng location : locations) {
                boundingBoxBuilder.include(location);
            }
            map.moveCamera(CameraUpdateFactory.newLatLngBounds(boundingBoxBuilder.build(), 100));
        }
    }

    // Lifecycle methods

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    /**
     * Rotation or screen size changed.
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
}