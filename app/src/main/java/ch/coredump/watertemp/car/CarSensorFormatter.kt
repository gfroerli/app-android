package ch.coredump.watertemp.car

import android.content.Context
import android.text.Spannable
import android.text.SpannableStringBuilder
import androidx.car.app.model.CarColor
import androidx.car.app.model.Distance
import androidx.car.app.model.DistanceSpan
import androidx.car.app.model.ForegroundCarColorSpan
import androidx.compose.ui.graphics.toArgb
import ch.coredump.watertemp.R
import ch.coredump.watertemp.activities.map.MarkerType
import ch.coredump.watertemp.rest.models.ApiSensor
import org.ocpsoft.prettytime.PrettyTime
import java.time.ZonedDateTime
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Convert a marker color to a [CarColor] (same color for light and dark themes).
 */
fun MarkerType.toCarColor(): CarColor = CarColor.createCustom(color.toArgb(), color.toArgb())

/**
 * Formatting helpers for the Android Auto screens.
 */
object CarSensorFormatter {
    private const val SEPARATOR = " · "

    /**
     * Format a temperature like "21.3 °C". Return null if the temperature is missing.
     */
    fun formatTemperature(temperature: Float?, locale: Locale = Locale.getDefault()): String? =
        temperature?.let { String.format(locale, "%.1f °C", it) }

    /**
     * Format the measurement age like "2 hours ago". Return null if the timestamp
     * is missing.
     */
    fun measuredAt(timestamp: ZonedDateTime?): String? =
        timestamp?.let { PrettyTime().format(it) }

    /**
     * Convert a distance in meters to a car [Distance] for display.
     */
    fun carDistance(distanceMeters: Float): Distance =
        if (distanceMeters < 1000) {
            Distance.create(distanceMeters.roundToInt().toDouble(), Distance.UNIT_METERS)
        } else {
            Distance.create(distanceMeters / 1000.0, Distance.UNIT_KILOMETERS)
        }

    /**
     * Build the secondary text line of a sensor list row, e.g. "21.3 °C · 1.2 km".
     *
     * The temperature comes first and is colored based on its value, falling back to a
     * "no measurement" text. The distance is omitted if [distanceMeters] is null.
     */
    fun rowText(context: Context, sensor: ApiSensor, distanceMeters: Float?): CharSequence {
        val builder = SpannableStringBuilder()

        // The temperature comes first, since that's the value a driver is looking for.
        // It is colored like the corresponding map marker.
        val temperature = formatTemperature(sensor.latestTemperature)
        if (temperature != null) {
            builder.append(
                temperature,
                ForegroundCarColorSpan.create(MarkerType.forTemperature(sensor.latestTemperature).toCarColor()),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        } else {
            builder.append(context.getString(R.string.no_measurement))
        }

        // Distance (placeholder character, replaced by the host with the formatted distance)
        if (distanceMeters != null) {
            builder.append(SEPARATOR)
            builder.append(
                " ",
                DistanceSpan.create(carDistance(distanceMeters)),
                Spannable.SPAN_INCLUSIVE_INCLUSIVE,
            )
        }

        return builder
    }
}
