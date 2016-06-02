package ch.coredump.watertemp;

import android.app.Activity;
import android.os.Bundle;

public class MapActivity extends Activity {

    private static final String TAG = "MapActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
    }
}
