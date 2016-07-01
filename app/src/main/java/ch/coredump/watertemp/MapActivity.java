package ch.coredump.watertemp;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;

import com.mapbox.mapboxsdk.MapboxAccountManager;

public class MapActivity extends Activity {

    private static final String TAG = "MapActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Start mapbox account manager
        MapboxAccountManager.start(this.getApplicationContext(), getString(R.string.mapbox_access_token));

        // Initialize the layout
        setContentView(R.layout.activity_map);
    }

    /**
     * Create a details fragment transaction with animations configured.
     */
    private FragmentTransaction getDetailsFragmentTransaction() {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.animator.slide_up, R.animator.slide_down);
        return transaction;
    }

    /**
     * Slide in details fragment.
     */
    public void showDetailsFragment() {
        FragmentTransaction transaction = getDetailsFragmentTransaction();
        Fragment detailsFragment = getFragmentManager().findFragmentById(R.id.fragment_details);
        transaction.show(detailsFragment);
        transaction.commit();
    }

    /**
     * Slide out details fragment.
     */
    public void hideDetailsFragment() {
        FragmentTransaction transaction = getDetailsFragmentTransaction();
        Fragment detailsFragment = getFragmentManager().findFragmentById(R.id.fragment_details);
        transaction.hide(detailsFragment);
        transaction.commit();
    }

}