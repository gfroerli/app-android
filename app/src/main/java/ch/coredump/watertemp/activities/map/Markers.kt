package ch.coredump.watertemp.activities.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import org.maplibre.android.maps.Style

enum class MarkerType(val color: Color) {
    // Unknown temperature
    UNKNOWN(Color(0xff777777)),

    // Below 10°C
    FREEZING(Color(0xff003c8f)),
    // 10-18°C
    COLD(Color(0xff1578bf)),
    // 18-21°C
    FRESH(Color(0xff98c1d9)),
    // 21-24°C
    NICE(Color(0xffe6c610)),
    // Above 24°C, for the real gfrörlis
    HOT(Color(0xffe27f29)),

    // Currently selected sensor
    ACTIVE(Color(0xffc62828));

    companion object {
        fun forTemperature(temperature: Float?): MarkerType {
            if (temperature == null) {
                return UNKNOWN;
            }
            if (temperature < 10f) {
                return FREEZING
            }
            if (temperature <= 18f) {
                return COLD
            }
            if (temperature <= 21) {
                return FRESH
            }
            if (temperature <= 24) {
                return NICE
            }
            return HOT
        }
    }
}

/**
 * Creates a circular marker using ShapeDrawable.
 */
fun createCircularMarker(
    size: Int,
    fillColor: Int,
    strokeColor: Int,
    strokeWidth: Int
): Bitmap {
    // Create bitmap and canvas
    val bitmap = createBitmap(size, size)
    val canvas = Canvas(bitmap)

    // Draw the shape
    val shapeDrawable = ShapeDrawable(OvalShape()).apply {
        intrinsicWidth = size
        intrinsicHeight = size
        paint.color = fillColor
        paint.isAntiAlias = true
    }
    shapeDrawable.setBounds(0, 0, size, size)
    shapeDrawable.draw(canvas)

    // Add stroke if needed
    if (strokeWidth > 0) {
        val strokePaint = Paint().apply {
            color = strokeColor
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth.toFloat()
            isAntiAlias = true
        }
        val radius = (size - strokeWidth) / 2f
        canvas.drawCircle(size / 2f, size / 2f, radius, strokePaint)
    }

    return bitmap
}

fun addStyleMarker(style: Style, markerType: MarkerType) {
    val markerSize = 72
    val strokeWidth = 8
    val fillColor = "#BBFFFFFF".toColorInt()
    style.addImage(markerType.name, createCircularMarker(
        size = markerSize,
        fillColor = fillColor,
        strokeColor = markerType.color.toArgb(),
        strokeWidth = strokeWidth,
    ))
}