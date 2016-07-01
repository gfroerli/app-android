package ch.coredump.watertemp;

import android.app.Activity;
import android.os.Bundle;

import com.mapbox.mapboxsdk.MapboxAccountManager;

public class MapActivity extends Activity {

    private static final String TAG = "MapActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Start mapbox account manager
        MapboxAccountManager.start(this.getApplicationContext(), getString(R.string.mapbox_access_token));

        setContentView(R.layout.activity_map);
    }

}