package ch.coredump.watertemp.activities.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import ch.coredump.watertemp.R
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.engine.LocationEngine
import org.maplibre.android.location.engine.LocationEngineCallback
import org.maplibre.android.location.engine.LocationEngineRequest
import org.maplibre.android.location.engine.LocationEngineResult
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style

/**
 * Shows the user's position on the map and centers the camera on it.
 */
class MapActivityLocation(private val activity: ComponentActivity) {

    companion object {
        private const val TAG = "MapActivityLocation"

        /** Zoom level used when centering the map on the user's position. */
        private const val LOCATE_ME_ZOOM = 10.0

        /** Duration of the camera animation in milliseconds. */
        private const val CAMERA_ANIMATION_MS = 1000

        /** Interval of the location request used to wait for the first fix. */
        private const val FIX_INTERVAL_MS = 1_000L

        /** Give up if no position could be determined within this time. */
        private const val FIX_TIMEOUT_MS = 20_000L

        /** Location permissions, either of which is sufficient. */
        val PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    }

    private val handler = Handler(Looper.getMainLooper())

    /** Set while waiting for a location fix. */
    private var pendingFix: LocationEngineCallback<LocationEngineResult>? = null
    private var pendingFixEngine: LocationEngine? = null

    /**
     * Whether the user granted permission to access the location.
     */
    fun hasPermission(): Boolean = PERMISSIONS.any {
        ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Activate the location component, which shows the user's position on the map.
     *
     * This must be called whenever the map style was (re)loaded, since the component
     * adds its layers to the style. Does nothing without location permission.
     */
    @SuppressLint("MissingPermission") // Permission is checked through hasPermission()
    fun activateLocationComponent(map: MapLibreMap, style: Style) {
        if (!hasPermission()) {
            Log.d(TAG, "No location permission, not showing user position")
            return
        }

        Log.d(TAG, "Activating location component")
        val component = map.locationComponent
        component.activateLocationComponent(
            LocationComponentActivationOptions.builder(activity, style)
                .useDefaultLocationEngine(true)
                .build()
        )
        component.isLocationComponentEnabled = true
        component.renderMode = RenderMode.NORMAL

        // The camera is only moved when the user taps the "locate me" button
        component.cameraMode = CameraMode.NONE
    }

    /**
     * Center the map on the user's position.
     *
     * Requires granted location permission (see [hasPermission]).
     */
    fun locateMe(map: MapLibreMap) {
        val style = map.style
        if (style == null) {
            Log.w(TAG, "Map style not loaded yet")
            return
        }

        // Not activated yet if the permission was only just granted
        val component = map.locationComponent
        if (!component.isLocationComponentActivated) {
            activateLocationComponent(map, style)
        }

        // If a position is already known, move the camera right away
        val lastKnownLocation = component.lastKnownLocation
        if (lastKnownLocation != null) {
            moveCameraTo(map, lastKnownLocation)
            return
        }

        // Otherwise wait for the first fix
        waitForFix(map, component.locationEngine)
    }

    /**
     * Stop waiting for a location fix. Should be called when the activity is destroyed.
     */
    fun cancelPendingFix() {
        val engine = pendingFixEngine
        val callback = pendingFix
        if (engine != null && callback != null) {
            engine.removeLocationUpdates(callback)
        }
        handler.removeCallbacksAndMessages(null)
        pendingFix = null
        pendingFixEngine = null
    }

    /**
     * Request location updates until the first fix arrives, then center the map on it.
     */
    @SuppressLint("MissingPermission") // Permission is checked in locateMe() before the location component is activated
    private fun waitForFix(map: MapLibreMap, locationEngine: LocationEngine?) {
        if (locationEngine == null) {
            Log.w(TAG, "No location engine available")
            showLocationUnavailable()
            return
        }

        // Already waiting for a fix
        if (pendingFix != null) {
            return
        }

        Log.d(TAG, "Waiting for a location fix")
        val callback = object : LocationEngineCallback<LocationEngineResult> {
            override fun onSuccess(result: LocationEngineResult) {
                val location = result.lastLocation
                cancelPendingFix()
                if (location == null) {
                    showLocationUnavailable()
                    return
                }
                moveCameraTo(map, location)
            }

            override fun onFailure(exception: Exception) {
                Log.w(TAG, "Could not determine position: $exception")
                cancelPendingFix()
                showLocationUnavailable()
            }
        }
        pendingFix = callback
        pendingFixEngine = locationEngine
        locationEngine.requestLocationUpdates(
            LocationEngineRequest.Builder(FIX_INTERVAL_MS)
                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                .build(),
            callback,
            Looper.getMainLooper(),
        )

        // Don't wait forever
        handler.postDelayed({
            Log.w(TAG, "Timed out while waiting for a location fix")
            cancelPendingFix()
            showLocationUnavailable()
        }, FIX_TIMEOUT_MS)
    }

    private fun moveCameraTo(map: MapLibreMap, location: Location) {
        Log.d(TAG, "Centering map on user position")
        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(location.latitude, location.longitude),
                LOCATE_ME_ZOOM,
            ),
            CAMERA_ANIMATION_MS,
        )
    }

    private fun showLocationUnavailable() {
        Toast.makeText(
            activity,
            R.string.location_unavailable,
            Toast.LENGTH_SHORT,
        ).show()
    }
}
