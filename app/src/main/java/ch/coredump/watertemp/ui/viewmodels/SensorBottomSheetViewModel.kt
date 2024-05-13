package ch.coredump.watertemp.ui.viewmodels

import android.util.Log
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import ch.coredump.watertemp.rest.models.ApiMeasurement
import ch.coredump.watertemp.rest.models.ApiSensor
import ch.coredump.watertemp.rest.models.ApiSensorDetails
import ch.coredump.watertemp.rest.models.ApiSponsor
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
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
 * View model holding information about the currently selected sensor displayed in the bottom sheet.
 */
class SensorBottomSheetViewModel : ViewModel() {
    // Sensor information
    private val _sensor = MutableStateFlow<Sensor?>(null)
    val sensor = _sensor.asStateFlow()

    // Measurements
    private val _measurements = MutableStateFlow<List<Measurement>?>(null)
    val measurements = _measurements.asStateFlow()

    // Whether the bottom sheet is shown or not
    private val _showBottomSheet = MutableStateFlow(false)
    val showBottomSheet = _showBottomSheet.asStateFlow()

    // Chart model producer
    val modelProducer = measurements.map {
        val producer = CartesianChartModelProducer.build()
        Log.i("ViewModel", "run")
        it?.let {
            Log.i("ViewModel", "Updating value")
            producer.tryRunTransaction {
                lineSeries {
                    series(
                        it.map { measurement -> measurement.timestamp.toInstant().toEpochMilli() },
                        it.map { measurement -> measurement.temperature },
                    )
                }
            }
        }
        producer
    }

    /**
     * Overwrite the current sensor and reset associated measurements.
     */
    fun setSensor(sensor: Sensor) {
        this._sensor.value = sensor
        this._measurements.value = null
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
            this._measurements.value = measurements
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
                this._sensor.value = sensor.copy(statsAllTime = SensorStats(
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
            this._sensor.value = sensor.copy(sponsor = Sponsor(sponsor.name, sponsor.description, sponsor.logoUrl))
        }
    }

    /**
     * Show the bottom sheet.
     */
    fun showBottomSheet() {
        if (this._sensor.value != null) {
            this._showBottomSheet.value = true
        }
    }

    /**
     * Hide the bottom sheet.
     */
    fun hideBottomSheet() {
        this._showBottomSheet.value = false
    }

    /**
     * Clear inner data and close bottom sheet.
     */
    fun clearData() {
        this._sensor.value = null
        this._measurements.value = null
        this._showBottomSheet.value = false
    }

    companion object {
        fun fromSensor(sensor: Sensor): SensorBottomSheetViewModel {
            return SensorBottomSheetViewModel().apply {
                setSensor(sensor)
            }
        }
    }
}