package ch.coredump.watertemp.activities.map

import android.util.SparseArray
import ch.coredump.watertemp.rest.SensorWithMeasurements
import ch.coredump.watertemp.rest.models.ApiSensor
import ch.coredump.watertemp.rest.models.ApiSponsor

/**
 * Data holder class for MapActivity's core data structures.
 * Encapsulates sensors and sponsors data to improve separation of concerns.
 */
class MapData {
    // Mapping from sensor IDs to `SensorMeasurements` instances
    private val sensors = HashMap<Int, SensorWithMeasurements>()

    // Mapping from sponsor IDs to `Sponsor` instances
    private val sponsors = SparseArray<ApiSponsor>()

    /**
     * Add a sensor without measurements.
     *
     * The measurements list will be initialized as empty.
     */
    fun addSensor(sensor: ApiSensor) {
        sensors[sensor.id] = SensorWithMeasurements(sensor)
    }
    /**
     * Add a sensor with measurements.
     */
    fun addSensorWithMeasurements(sensorId: Int, sensorWithMeasurement: SensorWithMeasurements) {
        sensors[sensorId] = sensorWithMeasurement
    }

    /**
     * Return the sensor with the specified sensor ID.
     */
    fun getSensor(sensorId: Int): ApiSensor? {
        return sensors[sensorId]?.sensor
    }

    /**
     * Return the sensor measurements for the specified sensor ID.
     */
    fun getSensorWithMeasurements(sensorId: Int): SensorWithMeasurements? {
        return sensors[sensorId]
    }

    /**
     * Return a list of all sensors.
     */
    fun getAllSensors(): List<ApiSensor> {
        return sensors.map { it.value.sensor }
    }

    /**
     * Return whether sensors are available.
     */
    fun hasSensors(): Boolean {
        return !sensors.isEmpty()
    }

    /**
     * Return the number of sensors available.
     */
    fun sensorCount(): Int {
        return sensors.size
    }

    /**
     * Clear only sensor data
     */
    fun clearSensors() {
        sensors.clear()
    }

    /**
     * Add a sponsor to the cache
     */
    fun addSponsor(sponsor: ApiSponsor) {
        sponsors.put(sponsor.id, sponsor)
    }

    /**
     * Get a cached sponsor by ID
     */
    fun getSponsor(sponsorId: Int): ApiSponsor? {
        return sponsors.get(sponsorId)
    }
}
