package ch.coredump.watertemp.car

import android.location.Location
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
 *
 * The sensor data and the position behind it are owned by [SensorListState], this
 * screen only renders them.
 */
class SensorListScreen(
    carContext: CarContext,
    private val repository: SensorRepository,
) : Screen(carContext) {

    companion object {
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
    }

    private val state = SensorListState(carContext, repository, lifecycle) { invalidate() }

    init {
        state.start()
    }

    override fun onGetTemplate(): Template = when (val current = state.current()) {
        is ListState.Failed -> errorTemplate()
        is ListState.RequestingPermission -> permissionHintTemplate()
        is ListState.Loading -> listTemplateBuilder().setLoading(true).build()
        is ListState.Ready -> sensorListTemplate(current.sensors, current.location)
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
                    .setOnClickListener { state.retry() }
                    .build()
            )
            .build()

    /**
     * The list of sensors, next to the map. The sensors are expected to be in the
     * order they should be shown in, see [SensorListState.current].
     */
    private fun sensorListTemplate(sorted: List<ApiSensor>, location: Location?): Template {
        // Show the closest sensors, but never more rows than the car host allows.
        // Note that the ConstraintManager requires car API level 2, but falls back to a
        // default limit on older hosts, so this does not raise our minCarApiLevel.
        val limit = carContext.getCarService(ConstraintManager::class.java)
            .getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_PLACE_LIST)
        val showPermissionWarning = !state.hasLocationPermission
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
        if (state.hasLocationPermission) {
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
        sensor.latestMeasurementAt?.let {
            row.addText(CarSensorFormatter.measuredAt(it))
        }

        return row.build()
    }
}
