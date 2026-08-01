package ch.coredump.watertemp.car

import org.junit.Assert
import org.junit.Test
import java.util.Locale

class CarSensorFormatterTest {
    @Test
    fun `format temperature with one decimal`() {
        Assert.assertEquals("21.3 °C", CarSensorFormatter.formatTemperature(21.32f, Locale.ENGLISH))
    }

    @Test
    fun `format temperature rounds up`() {
        Assert.assertEquals("21.4 °C", CarSensorFormatter.formatTemperature(21.36f, Locale.ENGLISH))
    }

    @Test
    fun `format temperature uses locale decimal separator`() {
        Assert.assertEquals("21,3 °C", CarSensorFormatter.formatTemperature(21.32f, Locale.GERMAN))
    }

    @Test
    fun `format missing temperature`() {
        Assert.assertNull(CarSensorFormatter.formatTemperature(null))
    }
}
