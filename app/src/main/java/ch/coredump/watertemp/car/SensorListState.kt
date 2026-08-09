package ch.coredump.watertemp.car

import android.location.Location
import android.location.LocationListener
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.car.app.CarContext
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import ch.coredump.watertemp.rest.SensorRepository
import ch.coredump.watertemp.rest.models.ApiSensor
import kotlin.math.roundToInt

/**
 * What the sensor list should currently show.
 */
sealed interface ListState {
    /** Waiting for the sensors, or for the first position to sort them by. */
    data object Loading : ListState

    /** The location permission dialog is shown on the phone. */
    data object RequestingPermission : ListState

    /** The sensors could not be loaded, and there is nothing to show instead. */
    data object Failed : ListState

    /**
     * The sensors in the order they should be shown.
     *
     * [location] is null if no position could be determined, in which case the sensors
     * are sorted by name instead of by distance.
     */
    data class Ready(val sensors: List<ApiSensor>, val location: Location?) : ListState
}

/**
 * The sensor data and the position behind the [SensorListScreen].
 *
 * Keeps both up to date while the screen is shown, and reports through [onChanged]
 * whenever [current] would return something new.
 */
class SensorListState(
    private val carContext: CarContext,
    private val repository: SensorRepository,
    private val lifecycle: Lifecycle,
    private val onChanged: () -> Unit,
) {

    companion object {
        private const val TAG = "SensorListState"

        /**
         * Give up waiting for a position after this time, and show the list without
         * distances instead.
         */
        private const val LOCATION_TIMEOUT_MS = 20 * 1000L

        /**
         * Reload the sensors once the user moved this far from the position the list
         * was sorted for. Without that, a long drive would keep showing the sensors
         * that happened to be closest when the app was started.
         */
        private const val REFRESH_DISTANCE_M = 10 * 1000f

        /** How often the sensor data is reloaded while the screen is shown. */
        private const val REFRESH_INTERVAL_MS = 60 * 1000L
    }

    /** How a sensor load affects what is currently on screen. */
    private enum class LoadMode {
        /** Initial load or retry: Show the loading indicator, report failures. */
        INITIAL,

        /** Periodic reload: Only update the values of the rows, keeping their order. */
        REFRESH,

        /** Reload after moving: Also sort the rows for the new position. */
        RESORT,
    }

    /** Fetched sensors (only ones that can be shown), or null while loading. */
    private var sensors: List<ApiSensor>? = null
    private var location: Location? = null
    private var loadFailed = false

    /**
     * The sensors in the order they are shown. Frozen as soon as the position is
     * known: Rows that reorder while driving are hard to follow, and every reorder
     * counts against the number of templates the host allows per task, while
     * updating only the distances counts as a refresh.
     *
     * The order is renewed after moving [REFRESH_DISTANCE_M], see [refreshIfMovedFar].
     */
    private var displayOrder: List<ApiSensor>? = null

    /** The position that [displayOrder] was sorted for. */
    private var displayOrderLocation: Location? = null

    /** Whether a request for the sensor data is currently in flight. */
    private var loading = false

    /** Set while subscribed to position updates. */
    private var locationListener: LocationListener? = null

    /**
     * Whether we're still waiting for a first position. Until then the list is not
     * shown, since it could not be sorted by distance yet.
     */
    private var waitingForLocation = true

    /** Whether the permission dialog is currently shown on the phone. */
    private var requestingPermission = false

    private val handler = Handler(Looper.getMainLooper())
    private val locationTimeout = Runnable {
        Log.d(TAG, "No position determined, showing the list without distances")
        stopWaitingForLocation()
    }

    /** Reloads the sensor data every [REFRESH_INTERVAL_MS] while the screen is shown. */
    private val refreshTimer = object : Runnable {
        override fun run() {
            loadSensors(LoadMode.REFRESH)
            handler.postDelayed(this, REFRESH_INTERVAL_MS)
        }
    }

    /** Whether the user granted permission to access the location. */
    val hasLocationPermission: Boolean
        get() = CarLocationProvider.hasPermission(carContext)

    /**
     * Request the position and the sensors, and keep both up to date while the screen
     * is shown.
     *
     * Note that [onChanged] may already be called from within this method, since a
     * cached position is reported synchronously.
     */
    fun start() {
        handler.postDelayed(locationTimeout, LOCATION_TIMEOUT_MS)
        if (CarLocationProvider.hasPermission(carContext)) {
            fetchLocation()
        } else {
            requestingPermission = true
            CarLocationProvider.requestPermission(carContext) { granted ->
                // Replace the hint telling the user to grant the permission. Fetching
                // the position usually reports back immediately (and invalidates), but
                // not if the system has no cached position yet, e.g. after a reboot.
                requestingPermission = false
                onChanged()

                if (granted) {
                    fetchLocation()
                    startLocationUpdates()
                } else {
                    // Without permission there is no position to wait for
                    stopWaitingForLocation()
                }
            }
        }
        loadSensors()

        // Keep the distances and the sensor data up to date while the screen is shown
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                startLocationUpdates()
                handler.postDelayed(refreshTimer, REFRESH_INTERVAL_MS)
            }

            override fun onStop(owner: LifecycleOwner) {
                stopLocationUpdates()
                handler.removeCallbacks(refreshTimer)
            }

            override fun onDestroy(owner: LifecycleOwner) {
                handler.removeCallbacks(locationTimeout)
                handler.removeCallbacks(refreshTimer)
                // Usually already done in onStop(), but not if the screen was never started
                stopLocationUpdates()
            }
        })
    }

    /** Load the sensors again after a failure. */
    fun retry() {
        loadSensors(LoadMode.INITIAL)
    }

    /**
     * What the screen should show right now.
     *
     * Note that this freezes the display order as a side effect, see [displayOrder].
     */
    fun current(): ListState {
        if (loadFailed) {
            return ListState.Failed
        }
        // The permission dialog is shown on the phone, so tell the user to look there
        if (requestingPermission && waitingForLocation) {
            return ListState.RequestingPermission
        }

        val sensors = this.sensors

        // Keep loading until the position is known: Without it, the list would show
        // arbitrary sensors instead of the closest ones.
        if (sensors == null || (location == null && waitingForLocation)) {
            return ListState.Loading
        }
        return ListState.Ready(sensorsInDisplayOrder(sensors, location), location)
    }

    private fun stopWaitingForLocation() {
        handler.removeCallbacks(locationTimeout)
        waitingForLocation = false
        onChanged()
    }

    /**
     * Fetch the sensors and show them sorted by distance.
     *
     * A reload in the background (any [mode] other than [LoadMode.INITIAL]) keeps the
     * current list on screen until the new sensors arrive, and keeps it on failure as
     * well: The list is useful even when slightly outdated, and losing it to a dropped
     * connection (e.g. in a tunnel) would be more annoying than helpful while driving.
     * The template is rebuilt in any case, so the relative measurement times keep
     * counting up even while the data cannot be reloaded.
     */
    private fun loadSensors(mode: LoadMode = LoadMode.INITIAL) {
        // Don't pile up requests if one is already on its way. A retry is exempt from
        // this, since the user explicitly asked for it.
        if (mode != LoadMode.INITIAL && loading) {
            return
        }

        loading = true
        if (mode == LoadMode.INITIAL) {
            loadFailed = false
            sensors = null
            displayOrder = null
            displayOrderLocation = null
            onChanged()
        }
        repository.loadFreshSensors { result ->
            loading = false
            result.fold(
                onSuccess = { fresh ->
                    val loaded = fresh.filter { it.canBeShownInCar() }
                    sensors = loaded
                    // A background reload recovers from a failed initial load
                    loadFailed = false
                    if (mode == LoadMode.REFRESH) {
                        applyToDisplayOrder(loaded)
                    } else {
                        // Sort the new sensors for the current position
                        displayOrder = null
                        displayOrderLocation = null
                    }
                },
                onFailure = { t ->
                    Log.e(TAG, "Fetching sensors failed: $t")
                    // Only show the error if there is nothing to show instead: A
                    // concurrent reload may have delivered a usable list in the
                    // meantime, which is more useful than an error screen.
                    if (mode == LoadMode.INITIAL && sensors == null) {
                        loadFailed = true
                    }
                },
            )
            onChanged()
        }
    }

    /**
     * Update the frozen [displayOrder] with the reloaded sensors, see
     * [mergeIntoDisplayOrder].
     */
    private fun applyToDisplayOrder(loaded: List<ApiSensor>) {
        val order = displayOrder ?: return
        val merged = mergeIntoDisplayOrder(order, loaded)
        if (merged == null) {
            // Sort the reloaded sensors for the current position instead
            displayOrder = null
            displayOrderLocation = null
            return
        }
        displayOrder = merged
    }

    /**
     * Reload the sensors once the user moved [REFRESH_DISTANCE_M] from the position
     * the list was sorted for, so that the list keeps showing the sensors nearby (with
     * up-to-date temperatures) instead of the ones near the start of the drive.
     */
    private fun refreshIfMovedFar(newLocation: Location) {
        // Return if a reload is already on its way
        if (loading) {
            return
        }

        // Only relevant once a position was determined and the list sorted for it
        val sortedFor = displayOrderLocation ?: return

        // Reload sensors when having moved
        val moved = sortedFor.distanceTo(newLocation)
        if (moved < REFRESH_DISTANCE_M) {
            return
        }
        Log.d(TAG, "Moved ${moved.roundToInt()} m since sorting the list, reloading sensors")
        loadSensors(LoadMode.RESORT)
    }

    private fun fetchLocation() {
        CarLocationProvider.getCurrentLocation(carContext) { newLocation ->
            // Show the list without distances if no position could be determined
            if (newLocation != null) {
                location = newLocation
            }
            stopWaitingForLocation()
        }
    }

    /**
     * Subscribe to position updates, unless already subscribed.
     *
     * Only subscribe while the screen is shown: The permission may be granted after
     * the screen was stopped, and in that case there is no [stopLocationUpdates] left
     * to unsubscribe. Position updates are started again in onStart().
     */
    private fun startLocationUpdates() {
        // Only start updates if permission was granted
        if (locationListener != null || !CarLocationProvider.hasPermission(carContext)) {
            return
        }

        // Do not subscribe if screen is not shown anymore
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            return
        }

        // Subscribe to updates
        locationListener = CarLocationProvider.startLocationUpdates(carContext) { newLocation ->
            location = newLocation
            stopWaitingForLocation()
            refreshIfMovedFar(newLocation)
        }
    }

    private fun stopLocationUpdates() {
        locationListener?.let { CarLocationProvider.stopLocationUpdates(carContext, it) }
        locationListener = null
    }

    /**
     * Order the sensors by distance, or by name as long as no position is known.
     *
     * The order is remembered as soon as a position is known, see [displayOrder].
     */
    private fun sensorsInDisplayOrder(sensors: List<ApiSensor>, location: Location?): List<ApiSensor> {
        displayOrder?.let { return it }
        if (location == null) {
            return sensors.sortedBy { it.deviceName.lowercase() }
        }

        val ordered = sensors.sortedBy { distanceMeters(location, it) }

        // Only keep this order if the position is up to date. An outdated cached
        // position may be far away, which would freeze the wrong sensors into the list.
        if (CarLocationProvider.isUpToDate(location)) {
            displayOrder = ordered
            displayOrderLocation = location
        }
        return ordered
    }
}

/**
 * Merge the reloaded sensors into the frozen display [order], so that the rows show the
 * new values without jumping around.
 *
 * Sensors whose measurements went stale drop out of the list. Newly added ones are
 * appended, they are sorted in with the next re-sort.
 *
 * Returns null if the order needs to be rebuilt from scratch, which is the case when
 * none of the previously shown sensors are left. That happens when the API temporarily
 * reports no fresh measurements at all: Appending all sensors would then show the
 * entire list in API order instead of by distance.
 */
internal fun mergeIntoDisplayOrder(
    order: List<ApiSensor>,
    loaded: List<ApiSensor>,
): List<ApiSensor>? {
    val loadedById = loaded.associateBy { it.id }
    val ordered = order.mapNotNull { loadedById[it.id] }
    if (ordered.isEmpty()) {
        return null
    }
    val orderedIds = ordered.mapTo(HashSet()) { it.id }
    return ordered + loaded.filter { it.id !in orderedIds }
}

/**
 * Whether a sensor can be shown in the car.
 *
 * Sensors without coordinates cannot be placed on the map, and the car templates
 * reject rows with an empty title (which an empty device name would produce).
 */
internal fun ApiSensor.canBeShownInCar(): Boolean =
    latitude != null && longitude != null && deviceName.isNotBlank()

/**
 * The distance between a position and a sensor, in meters.
 */
internal fun distanceMeters(location: Location, sensor: ApiSensor): Float {
    val results = FloatArray(1)
    Location.distanceBetween(
        location.latitude,
        location.longitude,
        sensor.latitude!!,
        sensor.longitude!!,
        results,
    )
    return results[0]
}
