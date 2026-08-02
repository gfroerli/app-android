package ch.coredump.watertemp.car

import android.location.Location
import android.location.LocationListener
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
import androidx.lifecycle.LifecycleOwner
import ch.coredump.watertemp.R
import ch.coredump.watertemp.activities.map.MarkerType
import ch.coredump.watertemp.rest.SensorRepository
import ch.coredump.watertemp.rest.models.ApiSensor

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
     */
    private var displayOrder: List<ApiSensor>? = null

    /** Set while subscribed to position updates. */
    private var locationListener: LocationListener? = null

    init {
        if (CarLocationProvider.hasPermission(carContext)) {
            fetchLocation()
        } else {
            CarLocationProvider.requestPermission(carContext) { granted ->
                if (granted) {
                    fetchLocation()
                    startLocationUpdates()
                }
            }
        }
        loadSensors()

        // Keep the distances up to date while the screen is shown
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                startLocationUpdates()
            }

            override fun onStop(owner: LifecycleOwner) {
                stopLocationUpdates()
            }
        })
    }

    private fun loadSensors() {
        loadFailed = false
        sensors = null
        displayOrder = null
        invalidate()
        repository.loadFreshSensors { result ->
            result.fold(
                onSuccess = { fresh ->
                    sensors = fresh.filter { it.latitude != null && it.longitude != null }
                },
                onFailure = { t ->
                    Log.e(TAG, "Fetching sensors failed: $t")
                    loadFailed = true
                },
            )
            invalidate()
        }
    }

    private fun fetchLocation() {
        CarLocationProvider.getCurrentLocation(carContext) { newLocation ->
            // Show the list without distances if no location is available
            if (newLocation != null) {
                location = newLocation
                invalidate()
            }
        }
    }

    private fun startLocationUpdates() {
        if (locationListener != null || !CarLocationProvider.hasPermission(carContext)) {
            return
        }
        locationListener = CarLocationProvider.startLocationUpdates(carContext) { newLocation ->
            location = newLocation
            invalidate()
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
        val sensors = this.sensors ?: return listTemplateBuilder().setLoading(true).build()
        return sensorListTemplate(sensors)
    }

    private fun listTemplateBuilder(): PlaceListMapTemplate.Builder =
        PlaceListMapTemplate.Builder()
            .setTitle(carContext.getString(R.string.car_sensor_list_title))
            .setHeaderAction(Action.APP_ICON)

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

        // Respect the row limit of the car host
        val limit = carContext.getCarService(ConstraintManager::class.java)
            .getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_PLACE_LIST)
        val shown = sorted.take(limit)

        val itemList = ItemList.Builder()
            .setNoItemsMessage(carContext.getString(R.string.car_no_sensors))
        for ((index, sensor) in shown.withIndex()) {
            itemList.addItem(sensorRow(sensor, location, number = index + 1))
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
     * Order the sensors by distance, or by name as long as no position is known.
     *
     * The order is remembered as soon as a position is known, see [displayOrder].
     */
    private fun sensorsInDisplayOrder(sensors: List<ApiSensor>, location: Location?): List<ApiSensor> {
        displayOrder?.let { return it }
        if (location == null) {
            return sensors.sortedBy { it.deviceName.lowercase() }
        }
        return sensors.sortedBy { distanceMeters(location, it) }.also { displayOrder = it }
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
