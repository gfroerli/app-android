package ch.coredump.watertemp;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.vision.text.Text;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.coredump.watertemp.rest.ApiClient;
import ch.coredump.watertemp.rest.ApiService;
import ch.coredump.watertemp.rest.SensorMeasurements;
import ch.coredump.watertemp.rest.models.Measurement;
import ch.coredump.watertemp.rest.models.Sensor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "MapFragment";

    private MapboxMap map;
    private MapView mapView;
    private ApiService apiService;
    private Map<Integer, SensorMeasurements> sensors = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.i(TAG, "Initializing fragment");

        // Inflate view
        final View view = inflater.inflate(R.layout.fragment_map, container, false);

        // Create map view
        mapView = (MapView) view.findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);

        // Initialize map
        mapView.getMapAsync(this);

        // Get API client
        // TODO: Use singleton dependency injection using something like dagger 2
        final ApiClient apiClient = new ApiClient();
        apiService = apiClient.getApiService();

        return view;
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
                        sensors.get(measurement.getSensorId()).addMeasurement(measurement);
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
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    private void updateMarkers() {
        // Create bounding box builder
        LatLngBounds.Builder boundingBoxBuilder = new LatLngBounds.Builder();

        // Process sensors
        for (SensorMeasurements sensorMeasurement : sensors.values()) {
            final Sensor sensor = sensorMeasurement.getSensor();
            final List<Measurement> measurements = sensorMeasurement.getMeasurements();
            Log.i(TAG, "Add sensor" + sensor.getCaption());

            // Sort measurements by ID
            Collections.sort(measurements, new Comparator<Measurement>() {
                @Override
                public int compare(Measurement lhs, Measurement rhs) {
                    final Integer leftId = new Integer(lhs.getId());
                    final Integer rightId = new Integer(rhs.getId());
                    return leftId.compareTo(rightId);
                }
            });

            // Create location object
            final float lat = sensor.getLocation().getLatitude();
            final float lng = sensor.getLocation().getLongitude();
            final LatLng location = new LatLng(lat, lng);

            // Build caption
            final StringBuilder captionBuilder = new StringBuilder();
            captionBuilder.append(sensor.getCaption());
            if (measurements.size() > 0) {
                captionBuilder.append('\n');
                captionBuilder.append(measurements.get(measurements.size() - 1).getTemperature());
                captionBuilder.append("°C");
            }

            // Add the marker to the map
            final Marker marker = map.addMarker(
                    new MarkerOptions()
                            .position(new LatLng(lat, lng))
                            .title(sensor.getCaption())
                            .snippet(captionBuilder.toString())
            );

            map.setOnMarkerClickListener(new MapboxMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(@NonNull Marker marker) {
                    Log.i(TAG, "Marker ID: " + marker.getId());
                    final TextView title = (TextView) getActivity().findViewById(R.id.details_title);
                    final TextView measurement = (TextView) getActivity().findViewById(R.id.details_measurement);
                    title.setText(marker.getTitle());
                    measurement.setText(marker.getSnippet());
                    return true;
                }
            });

            // Add the location to the bounding box
            boundingBoxBuilder.include(location);
        }

        // Change zoom to include all markers
        map.moveCamera(CameraUpdateFactory.newLatLngBounds(boundingBoxBuilder.build(), 100));
    }

}
