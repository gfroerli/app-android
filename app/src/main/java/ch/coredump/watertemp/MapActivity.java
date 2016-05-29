package ch.coredump.watertemp;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import java.util.List;

import ch.coredump.watertemp.rest.ApiClient;
import ch.coredump.watertemp.rest.ApiService;
import ch.coredump.watertemp.rest.models.Sensor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapActivity extends Activity implements OnMapReadyCallback {

    private static final String TAG = "MapActivity";

    private MapboxMap map;
    private MapView mapView;
    private ApiClient apiClient;

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
        apiClient = new ApiClient();
        ApiService apiService = apiClient.getApiService();

        // Fetch sensors
        Call<List<Sensor>> call = apiService.listSensors();
        call.enqueue(onSensorsFetched());
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        map = mapboxMap;

        // Add markers
        map.addMarker(
                new MarkerOptions()
                    .position(new LatLng(47.222331, 8.816589))
                    .title("HSR Badewiese")
                    .snippet("Wiese hinter der HSR.")
        );
        map.addMarker(
                new MarkerOptions()
                        .position(new LatLng(47.227723, 8.812858))
                        .title("Seebad Rapperswil")
                        .snippet("Gleich hinter dem Schloss.")
        );
        map.addMarker(
                new MarkerOptions()
                        .position(new LatLng(47.215407, 8.844550))
                        .title("Strandbad Stampf")
                        .snippet("21.8°C\nSponsored by HSR.")
        );
    }

    private Callback<List<Sensor>> onSensorsFetched() {
        return new Callback<List<Sensor>>() {
            @Override
            public void onResponse(Call<List<Sensor>> call, Response<List<Sensor>> response) {
                Log.i(TAG, "Response done!");
                if (response != null) {
                    for (Sensor sensor : response.body()) {
                        Log.i(TAG, "Sensor" + sensor.getDeviceName() + " at " + sensor.getLocation().toString());
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Sensor>> call, Throwable t) {
                Log.e(TAG, "Fetching failed:" + t.toString());
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

}
