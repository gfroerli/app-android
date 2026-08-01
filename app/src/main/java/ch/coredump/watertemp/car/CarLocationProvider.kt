package ch.coredump.watertemp.car

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.car.app.CarContext
import androidx.core.content.ContextCompat

/**
 * Location access for the Android Auto screens (no Play Services dependency).
 */
object CarLocationProvider {

    private const val TAG = "CarLocationProvider"

    /** Location permissions, either of which is sufficient. */
    val PERMISSIONS = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    /**
     * Location providers, ordered by preference.
     *
     * The network provider is queried first: It returns a position within seconds,
     * while waiting for a GPS fix can take half a minute or may not succeed at all
     * (e.g. in a tunnel or indoors). Its accuracy is more than good enough to show
     * the distance to a lake.
     */
    private val PROVIDERS = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)

    /** If the cached position is younger than this, don't wait for a new one. */
    private const val MAX_CACHED_POSITION_AGE_MS = 10 * 60 * 1000L

    /**
     * Whether the user granted permission to access the location.
     */
    fun hasPermission(context: Context): Boolean = PERMISSIONS.any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Request the location permission through the car host. The prompt is shown on the
     * phone, the callback is invoked on the main thread.
     */
    fun requestPermission(carContext: CarContext, onResult: (granted: Boolean) -> Unit) {
        carContext.requestPermissions(PERMISSIONS) { granted, _ ->
            onResult(granted.isNotEmpty())
        }
    }

    /**
     * Fetch the current position asynchronously. Requires the location permission.
     *
     * To avoid making the user wait for a fix, [onResult] may be invoked twice: First
     * with an outdated cached position, then with the current one once it is available.
     * It is invoked with null if no position could be determined at all.
     */
    // Permission is checked through hasPermission() before this is called
    @SuppressLint("MissingPermission")
    fun getCurrentLocation(context: Context, onResult: (Location?) -> Unit) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = PROVIDERS.filter { locationManager.isProviderEnabled(it) }
        if (providers.isEmpty()) {
            Log.w(TAG, "No location provider enabled")
            onResult(null)
            return
        }

        // A recently cached position is good enough to show distances, and is
        // available immediately.
        val cachedPosition = providers
            .mapNotNull { locationManager.getLastKnownLocation(it) }
            .maxByOrNull { it.time }
        if (cachedPosition != null &&
            System.currentTimeMillis() - cachedPosition.time < MAX_CACHED_POSITION_AGE_MS
        ) {
            Log.d(TAG, "Using cached position from ${cachedPosition.provider}")
            onResult(cachedPosition)
            return
        }

        // Show the outdated position right away, so that distances don't have to wait
        // for a fix, which can take a while.
        if (cachedPosition != null) {
            Log.d(TAG, "Using outdated cached position from ${cachedPosition.provider}")
            onResult(cachedPosition)
        }

        // Request an up-to-date position. Note that a provider may be enabled but
        // unable to determine a position (e.g. GPS indoors), so try all of them.
        Log.d(TAG, "Requesting position from providers: $providers")
        requestPosition(context, locationManager, providers) { position ->
            if (position != null) {
                onResult(position)
            } else if (cachedPosition == null) {
                Log.w(TAG, "Could not determine position")
                onResult(null)
            }
        }
    }

    /**
     * Request a position from the first provider that returns one.
     */
    @SuppressLint("MissingPermission")
    private fun requestPosition(
        context: Context,
        locationManager: LocationManager,
        providers: List<String>,
        onResult: (Location?) -> Unit,
    ) {
        val provider = providers.firstOrNull()
        if (provider == null) {
            onResult(null)
            return
        }

        // Try the remaining providers if this one doesn't return a position
        val onProviderResult: (Location?) -> Unit = { position ->
            if (position != null) {
                Log.d(TAG, "Got position from $provider")
                onResult(position)
            } else {
                requestPosition(context, locationManager, providers.drop(1), onResult)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val executor = ContextCompat.getMainExecutor(context)
            locationManager.getCurrentLocation(provider, null, executor) {
                onProviderResult(it)
            }
        } else {
            @Suppress("DEPRECATION")
            locationManager.requestSingleUpdate(
                provider,
                { onProviderResult(it) },
                Looper.getMainLooper(),
            )
        }
    }
}
