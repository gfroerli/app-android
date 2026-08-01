package ch.coredump.watertemp.car

import android.location.Location
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

    init {
        if (CarLocationProvider.hasPermission(carContext)) {
            fetchLocation()
        } else {
            CarLocationProvider.requestPermission(carContext) { granted ->
                if (granted) {
                    fetchLocation()
                }
            }
        }
        loadSensors()
    }

    private fun loadSensors() {
        loadFailed = false
        sensors = null
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
        val sorted = if (location != null) {
            sensors.sortedBy { distanceMeters(location, it) }
        } else {
            sensors.sortedBy { it.deviceName.lowercase() }
        }

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
        if (location != null) {
            builder.setCurrentLocationEnabled(true)
        }
        if (shown.isNotEmpty()) {
            // Anchor the map at the centroid of the shown sensors. The host adapts the
            // camera to keep the anchor and the visible rows' markers in the viewport,
            // so without an anchor it may zoom in on a single marker.
            builder.setAnchor(
                Place.Builder(
                    CarLocation.create(
                        shown.mapNotNull { it.latitude }.average(),
                        shown.mapNotNull { it.longitude }.average(),
                    )
                ).build()
            )
        }
        return builder.build()
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
            .setOnClickListener { screenManager.push(SensorDetailScreen(carContext, sensor)) }

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
