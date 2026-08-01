package ch.coredump.watertemp.car

import android.content.Intent
import android.net.Uri
import androidx.car.app.CarContext
import androidx.core.net.toUri
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import ch.coredump.watertemp.R
import ch.coredump.watertemp.rest.models.ApiSensor

/**
 * Android Auto detail screen for a single sensor, with an action to navigate to it
 * using the car's navigation app.
 */
class SensorDetailScreen(
    carContext: CarContext,
    private val sensor: ApiSensor,
) : Screen(carContext) {

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
     * Hand off to the car's navigation app.
     */
    private fun navigateToSensor() {
        val uri = "geo:0,0?q=${sensor.latitude},${sensor.longitude}(${Uri.encode(sensor.deviceName)})".toUri()
        carContext.startCarApp(Intent(CarContext.ACTION_NAVIGATE, uri))
    }
}
