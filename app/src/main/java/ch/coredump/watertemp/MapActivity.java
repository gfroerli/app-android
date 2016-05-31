package ch.coredump.watertemp;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.coredump.watertemp.rest.ApiClient;
import ch.coredump.watertemp.rest.ApiService;
import ch.coredump.watertemp.rest.models.Measurement;
import ch.coredump.watertemp.rest.models.Sensor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapActivity extends Activity implements OnMapReadyCallback {

    private static final String TAG = "MapActivity";

    private MapboxMap map;
    private MapView mapView;
    private ApiService apiService;
    private Map<Sensor, List<Measurement>> sensors = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // Create map view
        mapView = (MapView) findViewById(R.id.mapview);
        mapView.onCreate(savedInstanceState);

        // Initialize map
        mapView.getMapAsync(this);

        // Get API client
        // TODO: Use singleton dependency injection using something like dagger 2
        final ApiClient apiClient = new ApiClient();
        apiService = apiClient.getApiService();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        // Save map as attribute
        map = mapboxMap;

        // Fetch sensors
        Call<List<Sensor>> sensorCall = apiService.listSensors();
        sensorCall.enqueue(onSensorsFetched());
    }

    private Callback<List<Sensor>> onSensorsFetched() {
        return new Callback<List<Sensor>>() {
            @Override
            public void onResponse(Call<List<Sensor>> call, Response<List<Sensor>> response) {
                Log.i(TAG, "Sensor response done!");
                if (response != null) {
                    for (Sensor sensor : response.body()) {
                        sensors.put(sensor, new ArrayList<Measurement>());
                    }

                    // Fetch measurements
                    List<String> idList = new ArrayList<>();
                    for (Sensor sensor : sensors.keySet()) { // Oh,verbose Java! Where is your .map()?
                        idList.add(String.valueOf(sensor.getId()));
                    }
                    final String ids = Utils.join(",", idList);
                    Call<List<Measurement>> measurementCall = apiService.listMeasurements(ids, 10);
                    measurementCall.enqueue(onMeasurementsFetched());
                }
            }

            @Override
            public void onFailure(Call<List<Sensor>> call, Throwable t) {
                Log.e(TAG, "Fetching sensors failed:" + t.toString());
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
                    }
                    updateMarkers();
                }
            }

            @Override
            public void onFailure(Call<List<Measurement>> call, Throwable t) {
                Log.e(TAG, "Fetching measurements failed:" + t.toString());
            }
        };
    }

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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
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

    private void updateMarkers() {
        for (Sensor sensor : sensors.keySet()) {
            Log.i(TAG, "Add sensor" + sensor.getDeviceName());
            final float lat = sensor.getLocation().getLatitude();
            final float lng = sensor.getLocation().getLongitude();
            map.addMarker(
                    new MarkerOptions()
                            .position(new LatLng(lat, lng))
                            .title(sensor.getDeviceName())
                            .snippet(sensor.getCaption())
            );
        }
    }

}
