package ch.coredump.watertemp.ui.viewmodels

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import ch.coredump.watertemp.rest.models.ApiMeasurement
import ch.coredump.watertemp.rest.models.ApiSensor
import ch.coredump.watertemp.rest.models.ApiSensorDetails
import ch.coredump.watertemp.rest.models.ApiSponsor
import java.time.ZonedDateTime

data class Measurement(
    val timestamp: ZonedDateTime,
    val temperature: Float,
) {
    companion object {
        fun fromApiMeasurement(measurement: ApiMeasurement): Measurement {
            return Measurement(measurement.createdAt, measurement.temperature)
        }
    }
}

data class SensorStats(
    val minTemp: Double,
    val maxTemp: Double,
    val avgTemp: Double,
)

data class Sponsor(
    val name: String,
    val description: String?,
    val logoUrl: String?,
)

data class Sensor(
    val name: String,
    val caption: String?,
    val latestMeasurement: Measurement?,
    val statsAllTime: SensorStats? = null,
    val sponsor: Sponsor? = null,
) {
    companion object {
        fun fromApiSensor(sensor: ApiSensor): Sensor {
            var latestMeasurement: Measurement? = null
            if (sensor.latestTemperature != null && sensor.latestMeasurementAt != null) {
                latestMeasurement = Measurement(
                    sensor.latestMeasurementAt,
                    sensor.latestTemperature,
                )
            }
            return Sensor(sensor.deviceName, sensor.caption, latestMeasurement, null)
        }
    }
}

/**
 * View model holding information about the currently selected sensor.
 */
data class SensorViewModel(
    val sensor: MutableState<Sensor?> = mutableStateOf(null),
    val measurements: MutableState<List<Measurement>?> = mutableStateOf(null),
) : ViewModel() {
    /**
     * Overwrite the current sensor and reset associated measurements.
     */
    fun setSensor(sensor: Sensor) {
        this.sensor.value = sensor
        this.measurements.value = null
    }

    /**
     * Overwrite the current measurements.
     *
     * Previous measurements will be overwritten, not merged.
     *
     * If no sensor is set, this is a no-op.
     */
    fun setMeasurements(measurements: List<Measurement>) {
        if (this.sensor.value != null) {
            this.measurements.value = measurements
        }
    }

    /**
     * Add sensor details to an already existing sensor.
     *
     * If no sensor is set, this is a no-op.
     */
    fun addDetails(details: ApiSensorDetails) {
        this.sensor.value?.let { sensor ->
            if (details.minimumTemperature != null && details.maximumTemperature != null && details.averageTemperature != null) {
                this.sensor.value = sensor.copy(statsAllTime = SensorStats(
                    details.minimumTemperature,
                    details.maximumTemperature,
                    details.averageTemperature
                ))
            }
        }
    }

    /**
     * Add sponsor details to an already existing sensor.
     *
     * If no sensor is set, this is a no-op.
     */
    fun addSponsor(sponsor: ApiSponsor) {
        this.sensor.value?.let { sensor ->
            this.sensor.value = sensor.copy(sponsor = Sponsor(sponsor.name, sponsor.description, sponsor.logoUrl))
        }
    }

    /**
     * Clear inner data.
     */
    fun clear() {
        this.sensor.value = null
        this.measurements.value = null
    }

    companion object {
        fun fromSensor(sensor: Sensor): SensorViewModel {
            return SensorViewModel(mutableStateOf(sensor), mutableStateOf(null))
        }
    }
}