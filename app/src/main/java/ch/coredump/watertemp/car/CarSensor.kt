package ch.coredump.watertemp.car

import ch.coredump.watertemp.rest.models.ApiSensor
import java.time.ZonedDateTime

/**
 * A sensor that can be shown in the car.
 *
 * The car templates need coordinates to place a marker on the map, and reject rows with
 * an empty title. Both are optional in the API model, so they are checked once in
 * [CarSensor.of] instead of at every place that renders a sensor.
 */
class CarSensor private constructor(
    /** The API model, for requests that take a whole sensor. */
    val api: ApiSensor,
    val latitude: Double,
    val longitude: Double,
    val deviceName: String,
) {
    val id: Int get() = api.id
    val caption: String? get() = api.caption
    val latestTemperature: Float? get() = api.latestTemperature
    val latestMeasurementAt: ZonedDateTime? get() = api.latestMeasurementAt

    companion object {
        /**
         * Wrap a sensor, or return null if it cannot be shown in the car.
         */
        fun of(sensor: ApiSensor): CarSensor? {
            val latitude = sensor.latitude ?: return null
            val longitude = sensor.longitude ?: return null
            val deviceName = sensor.deviceName.takeIf { it.isNotBlank() } ?: return null
            return CarSensor(sensor, latitude, longitude, deviceName)
        }
    }
}
