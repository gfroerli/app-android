package ch.coredump.watertemp

import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.coredump.watertemp.activities.map.createCircularMarker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MarkersTest {

    @Test
    fun testCreateCircularMarkerSize() {
        val testSizes = listOf(16, 24, 32, 48, 64, 96, 128)

        for (size in testSizes) {
            val bitmap = createCircularMarker(
                size = size,
                fillColor = Color.RED,
                strokeColor = Color.BLACK,
                strokeWidth = 6
            )
            assertNotNull("Bitmap should not be null for size $size", bitmap)
            assertEquals("Bitmap width should be $size", size, bitmap.width)
            assertEquals("Bitmap height should be $size", size, bitmap.height)
        }
    }

    @Test
    fun testCreateCircularMarkerWithoutStroke() {
        val size = 40
        val bitmap = createCircularMarker(
            size = size,
            fillColor = Color.GREEN,
            strokeColor = Color.WHITE,
            strokeWidth = 0 // No stroke
        )

        assertNotNull("Bitmap should not be null", bitmap)
        assertEquals("Bitmap width should match size", size, bitmap.width)
        assertEquals("Bitmap height should match size", size, bitmap.height)
    }

    @Test
    fun testCreateCircularMarkerCenterPixelColor() {
        val size = 48
        val fillColor = Color.RED
        val bitmap = createCircularMarker(
            size = size,
            fillColor = fillColor,
            strokeColor = Color.WHITE,
            strokeWidth = 2
        )

        // Check center pixel should be fill color
        val centerX = size / 2
        val centerY = size / 2
        val centerPixel = bitmap.getPixel(centerX, centerY)

        assertEquals("Center pixel should be fill color", fillColor, centerPixel)
    }

    @Test
    fun testCreateCircularMarkerCornerPixelsAreTransparent() {
        val size = 48
        val bitmap = createCircularMarker(
            size = size,
            fillColor = Color.BLUE,
            strokeColor = Color.WHITE,
            strokeWidth = 2
        )

        // Check corner pixels should be transparent (outside circle)
        val cornerPixels = listOf(
            bitmap.getPixel(0, 0), // Top-left
            bitmap.getPixel(size - 1, 0), // Top-right
            bitmap.getPixel(0, size - 1), // Bottom-left
            bitmap.getPixel(size - 1, size - 1) // Bottom-right
        )

        for (pixel in cornerPixels) {
            assertEquals("Corner pixels should be transparent", Color.TRANSPARENT, pixel)
        }
    }

    @Test
    fun testCreateCircularMarkerStrokeColorAtEdge() {
        val size = 48
        val strokeWidth = 6
        val fillColor = Color.GREEN
        val strokeColor = Color.RED
        val bitmap = createCircularMarker(
            size = size,
            fillColor = fillColor,
            strokeColor = strokeColor,
            strokeWidth = strokeWidth
        )

        // Middle left pixel should have stroke color
        val strokePixel = bitmap.getPixel(strokeWidth / 2, size / 2)
        assertEquals("Edge pixel should be stroke color", strokeColor, strokePixel)
    }

    @Test
    fun testCreateCircularMarkerNoStrokeColorAtEdge() {
        val size = 48
        val fillColor = Color.GREEN
        val strokeColor = Color.RED
        val bitmap = createCircularMarker(
            size = size,
            fillColor = fillColor,
            strokeColor = strokeColor,
            strokeWidth = 0
        )

        // Middle left pixel should have stroke color
        val strokePixel = bitmap.getPixel(3, size / 2)
        assertEquals("Edge pixel should be fill color", fillColor, strokePixel)
    }
}
