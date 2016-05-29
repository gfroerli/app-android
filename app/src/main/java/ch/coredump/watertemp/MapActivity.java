package ch.coredump.watertemp;

import android.app.Activity;
import android.os.Bundle;

import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

public class MapActivity extends Activity implements OnMapReadyCallback {

    private MapboxMap mMap;
    private MapView mMapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // Create map view
        mMapView = (MapView) findViewById(R.id.mapview);
        mMapView.onCreate(savedInstanceState);

        // Initialize map
        mMapView.getMapAsync(this);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        mMap = mapboxMap;

        // Add markers
        mMap.addMarker(
                new MarkerOptions()
                    .position(new LatLng(47.222331, 8.816589))
                    .title("HSR Badewiese")
                    .snippet("Wiese hinter der HSR.")
        );
        mMap.addMarker(
                new MarkerOptions()
                        .position(new LatLng(47.227723, 8.812858))
                        .title("Seebad Rapperswil")
                        .snippet("Gleich hinter dem Schloss.")
        );
        mMap.addMarker(
                new MarkerOptions()
                        .position(new LatLng(47.215407, 8.844550))
                        .title("Strandbad Stampf")
                        .snippet("21.8°C\nSponsored by HSR.")
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
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

}
