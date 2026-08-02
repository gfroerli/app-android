package ch.coredump.watertemp.car

import android.content.Intent
import android.util.Log
import androidx.car.app.CarContext
import androidx.core.net.toUri
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import ch.coredump.watertemp.R
import ch.coredump.watertemp.rest.SensorRepository
import ch.coredump.watertemp.rest.models.ApiSensor
import ch.coredump.watertemp.rest.models.ApiSponsor
import ch.coredump.watertemp.rest.models.SponsorType

/**
 * Android Auto detail screen for a single sensor, with an action to navigate to it
 * using the car's navigation app.
 */
class SensorDetailScreen(
    carContext: CarContext,
    private val sensor: ApiSensor,
    private val repository: SensorRepository,
) : Screen(carContext) {

    companion object {
        private const val TAG = "SensorDetailScreen"
    }

    /** The sponsor, as soon as it was fetched. */
    private var sponsor: ApiSponsor? = null

    init {
        loadSponsor()
    }

    private fun loadSponsor() {
        // Skip the request for sensors without a sponsor. Note that the endpoint is
        // queried with the sensor ID, not with the sponsor ID.
        if (sensor.sponsorId == null) {
            return
        }
        repository.loadSponsor(sensor.id) { result ->
            result.fold(
                onSuccess = {
                    sponsor = it
                    invalidate()
                },
                // The sponsor is supplementary information, so just leave it out
                onFailure = { Log.w(TAG, "Fetching sponsor failed: $it") },
            )
        }
    }

    override fun onGetTemplate(): Template {
        val temperatureRow = Row.Builder()
            .setTitle(carContext.getString(R.string.temperature))
            .addText(
                CarSensorFormatter.formatTemperature(sensor.latestTemperature)
                    ?: carContext.getString(R.string.no_measurement)
            )
        CarSensorFormatter.measuredAt(sensor.latestMeasurementAt)?.let {
            temperatureRow.addText(it)
        }

        val pane = Pane.Builder().addRow(temperatureRow.build())
        sensor.caption?.let {
            pane.addRow(Row.Builder().setTitle(it).build())
        }
        sponsorRow()?.let { pane.addRow(it) }
        pane.addAction(
            Action.Builder()
                .setTitle(carContext.getString(R.string.car_action_navigate))
                .setOnClickListener { navigateToSensor() }
                .build()
        )

        return PaneTemplate.Builder(pane.build())
            .setTitle(sensor.deviceName)
            .setHeaderAction(Action.BACK)
            .build()
    }

    /**
     * A row naming the sponsor, or the source for publicly available data.
     * Null as long as the sponsor was not fetched (or could not be fetched).
     */
    private fun sponsorRow(): Row? {
        val sponsor = this.sponsor ?: return null
        val title = when (sponsor.sponsorType) {
            SponsorType.PublicDataProvider ->
                carContext.getString(R.string.section_header_data_source, sponsor.name)
            else ->
                carContext.getString(R.string.section_header_sponsor, sponsor.name)
        }
        return Row.Builder().setTitle(title).build()
    }

    /**
     * Hand off to the car's navigation app.
     */
    private fun navigateToSensor() {
        val uri = "geo:${sensor.latitude},${sensor.longitude}".toUri()
        carContext.startCarApp(Intent(CarContext.ACTION_NAVIGATE, uri))
    }
}
