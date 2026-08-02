package ch.coredump.watertemp.car

import android.location.Location
import android.location.LocationListener
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.constraints.ConstraintManager
import androidx.car.app.model.Action
import androidx.car.app.model.CarLocation
import androidx.car.app.model.ItemList
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Metadata
import androidx.car.app.model.Place
import androidx.car.app.model.PlaceListMapTemplate
import androidx.car.app.model.PlaceMarker
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import ch.coredump.watertemp.R
import ch.coredump.watertemp.activities.map.MarkerType
import ch.coredump.watertemp.rest.SensorRepository
import ch.coredump.watertemp.rest.models.ApiSensor
import kotlin.math.roundToInt

/**
 * Main Android Auto screen: a map with the sensor locations, next to a list of
 * sensors with their current temperatures.
 *
 * If the location permission is granted, sensors are sorted by distance and a
 * distance is shown per row. Otherwise sensors are sorted by name.
 */
class SensorListScreen(
    carContext: CarContext,
    private val repository: SensorRepository,
) : Screen(carContext) {

    companion object {
        private const val TAG = "SensorListScreen"

        /**
         * How many rows to show at most.
         *
         * The host only shows the markers of the currently visible rows, so a long
         * list means that most sensors are missing from the map. It also keeps the
         * template small enough to be sent to the host in a single binder
         * transaction, which is size limited.
         *
         * Note that the hint rows count towards this limit, so usually 20 sensors
         * are shown next to the hint about the remaining ones.
         */
        private const val MAX_ROWS = 21

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

    /** Fetched sensors (only ones with coordinates), or null while loading. */
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

    /** Whether sensors are currently being reloaded in the background. */
    private var refreshing = false

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

    init {
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
                invalidate()

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

    private fun stopWaitingForLocation() {
        handler.removeCallbacks(locationTimeout)
        waitingForLocation = false
        invalidate()
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
        // Don't pile up requests if one is already on its way
        if (mode != LoadMode.INITIAL && refreshing) {
            return
        }

        refreshing = mode != LoadMode.INITIAL
        if (mode == LoadMode.INITIAL) {
            loadFailed = false
            sensors = null
            displayOrder = null
            displayOrderLocation = null
            invalidate()
        }
        repository.loadFreshSensors { result ->
            refreshing = false
            result.fold(
                onSuccess = { fresh ->
                    val loaded = fresh.filter { it.latitude != null && it.longitude != null }
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
                    if (mode == LoadMode.INITIAL) {
                        loadFailed = true
                    }
                },
            )
            invalidate()
        }
    }

    /**
     * Update the frozen [displayOrder] with the reloaded sensors, so that the rows show
     * the new values without jumping around.
     *
     * Sensors whose measurements went stale drop out of the list. Newly added ones are
     * appended, they are sorted in with the next re-sort.
     */
    private fun applyToDisplayOrder(loaded: List<ApiSensor>) {
        val order = displayOrder ?: return
        val loadedById = loaded.associateBy { it.id }
        val ordered = order.mapNotNull { loadedById[it.id] }
        val orderedIds = ordered.mapTo(HashSet()) { it.id }
        displayOrder = ordered + loaded.filter { it.id !in orderedIds }
    }

    /**
     * Reload the sensors once the user moved [REFRESH_DISTANCE_M] from the position
     * the list was sorted for, so that the list keeps showing the sensors nearby (with
     * up-to-date temperatures) instead of the ones near the start of the drive.
     */
    private fun refreshIfMovedFar(newLocation: Location) {
        // Return if already refreshing
        if (refreshing) {
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

    override fun onGetTemplate(): Template {
        if (loadFailed) {
            return errorTemplate()
        }
        // The permission dialog is shown on the phone, so tell the user to look there
        if (requestingPermission && waitingForLocation) {
            return permissionHintTemplate()
        }

        val sensors = this.sensors

        // Keep loading until the position is known: Without it, the list would show
        // arbitrary sensors instead of the closest ones.
        if (sensors == null || (location == null && waitingForLocation)) {
            return listTemplateBuilder().setLoading(true).build()
        }
        return sensorListTemplate(sensors)
    }

    private fun listTemplateBuilder(): PlaceListMapTemplate.Builder =
        PlaceListMapTemplate.Builder()
            .setTitle(carContext.getString(R.string.car_sensor_list_title))
            .setHeaderAction(Action.APP_ICON)

    private fun permissionHintTemplate(): Template =
        MessageTemplate.Builder(carContext.getString(R.string.car_location_permission_hint))
            .setTitle(carContext.getString(R.string.car_sensor_list_title))
            .setHeaderAction(Action.APP_ICON)
            .build()

    private fun errorTemplate(): Template =
        MessageTemplate.Builder(carContext.getString(R.string.car_error_loading_sensors))
            .setTitle(carContext.getString(R.string.car_sensor_list_title))
            .setHeaderAction(Action.APP_ICON)
            .addAction(
                Action.Builder()
                    .setTitle(carContext.getString(R.string.car_action_retry))
                    .setOnClickListener { loadSensors() }
                    .build()
            )
            .build()

    private fun sensorListTemplate(sensors: List<ApiSensor>): Template {
        val location = this.location
        val sorted = sensorsInDisplayOrder(sensors, location)

        // Show the closest sensors, but never more rows than the car host allows
        val limit = carContext.getCarService(ConstraintManager::class.java)
            .getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_PLACE_LIST)
        val showPermissionWarning = !CarLocationProvider.hasPermission(carContext)
        val maxRows = minOf(limit, MAX_ROWS)
        val availableRows = (maxRows - if (showPermissionWarning) 1 else 0).coerceAtLeast(1)

        // If not all sensors fit, keep a row free for the hint about the missing ones
        val shown = if (sorted.size > availableRows) {
            sorted.take(availableRows - 1)
        } else {
            sorted
        }

        val itemList = ItemList.Builder()
            .setNoItemsMessage(carContext.getString(R.string.car_no_sensors))

        // Explain why the sensors are not sorted by distance. Not for an empty list,
        // where the sort order is meaningless and the row would take the place of the
        // "no sensors" message.
        if (showPermissionWarning && sorted.isNotEmpty()) {
            itemList.addItem(permissionWarningRow())
        }

        for ((index, sensor) in shown.withIndex()) {
            itemList.addItem(sensorRow(sensor, location, number = index + 1))
        }

        if (shown.size < sorted.size) {
            itemList.addItem(moreSensorsRow(shown.size, sorted.size))
        }

        val builder = listTemplateBuilder().setItemList(itemList.build())

        // The host shows the current position itself, it only requires the permission.
        // In particular this does not depend on us having determined a position.
        if (CarLocationProvider.hasPermission(carContext)) {
            builder.setCurrentLocationEnabled(true)
        }

        // The host keeps the anchor and the markers of the currently visible rows in
        // the viewport. Anchoring at the user's position keeps the map centered on the
        // surroundings, also while scrolling through rows.
        val anchor = when {
            location != null -> CarLocation.create(location.latitude, location.longitude)
            // Fall back to the first sensor, so that the map shows an actual location
            shown.isNotEmpty() -> CarLocation.create(shown[0].latitude!!, shown[0].longitude!!)
            else -> null
        }
        if (anchor != null) {
            builder.setAnchor(Place.Builder(anchor).build())
        }
        return builder.build()
    }

    /**
     * A row warning that the location permission is missing, leading to a screen
     * that explains the consequences.
     *
     * Note that the row must be browsable: The template requires a distance on all
     * other rows, which we cannot show without the permission.
     */
    private fun permissionWarningRow(): Row =
        Row.Builder()
            .setTitle(carContext.getString(R.string.car_location_permission_denied))
            .addText(carContext.getString(R.string.car_location_permission_denied_short))
            .setBrowsable(true)
            .setOnClickListener {
                screenManager.push(
                    MessageScreen(
                        carContext,
                        title = carContext.getString(R.string.car_location_permission_denied),
                        message = carContext.getString(R.string.car_location_permission_denied_text),
                    )
                )
            }
            .build()

    /**
     * A row telling that the list was shortened, leading to a screen that explains
     * where the remaining sensors can be seen.
     *
     * Like [permissionWarningRow], this row must be browsable, since it has no
     * distance to show.
     */
    private fun moreSensorsRow(shownCount: Int, totalCount: Int): Row =
        Row.Builder()
            .setTitle(carContext.getString(R.string.car_more_sensors))
            .addText(carContext.getString(R.string.car_more_sensors_short, shownCount, totalCount))
            .setBrowsable(true)
            .setOnClickListener {
                screenManager.push(
                    MessageScreen(
                        carContext,
                        title = carContext.getString(R.string.car_more_sensors),
                        message = carContext.getString(
                            R.string.car_more_sensors_text,
                            shownCount,
                            totalCount,
                        ),
                    )
                )
            }
            .build()

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

    private fun sensorRow(sensor: ApiSensor, location: Location?, number: Int): Row {
        val marker = PlaceMarker.Builder()
            .setColor(MarkerType.forTemperature(sensor.latestTemperature).toCarColor())
            // The marker is shown both on the map and in the list, so the number
            // allows matching a list entry to its position on the map.
            .setLabel(number.toString())
            .build()
        val distance = location?.let { distanceMeters(it, sensor) }
        val row = Row.Builder()
            .setTitle(sensor.deviceName)
            .addText(CarSensorFormatter.rowText(carContext, sensor, distance))
            // Rows lead to a detail screen. This also exempts them from the template
            // requirement that every row must show a distance (which we cannot
            // fulfill before a location fix, or when location permission is denied).
            .setBrowsable(true)
            .setMetadata(
                Metadata.Builder()
                    .setPlace(
                        Place.Builder(CarLocation.create(sensor.latitude!!, sensor.longitude!!))
                            .setMarker(marker)
                            .build()
                    )
                    .build()
            )
            .setOnClickListener {
                screenManager.push(SensorDetailScreen(carContext, sensor, repository))
            }

        // Second text line: how old the measurement is. The template allows two.
        CarSensorFormatter.measuredAt(sensor.latestMeasurementAt)?.let {
            row.addText(it)
        }

        return row.build()
    }

    private fun distanceMeters(location: Location, sensor: ApiSensor): Float {
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
}
