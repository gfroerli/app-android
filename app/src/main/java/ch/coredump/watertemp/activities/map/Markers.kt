package ch.coredump.watertemp.activities.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import androidx.core.graphics.createBitmap

enum class MarkerType {
    DEFAULT,
    ACTIVE,
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