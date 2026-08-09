package ch.coredump.watertemp.car

import ch.coredump.watertemp.rest.models.ApiSensor
import org.junit.Assert
import org.junit.Test
import java.time.ZonedDateTime

class SensorListStateTest {
    private fun sensor(
        deviceName: String = "Zürichsee",
        latitude: Double? = 47.2266,
        longitude: Double? = 8.8184,
    ) = ApiSensor(
        id = 1,
        deviceName = deviceName,
        caption = null,
        latitude = latitude,
        longitude = longitude,
        latestTemperature = 20.5f,
        latestMeasurementAt = ZonedDateTime.now(),
        sponsorId = null,
    )

    @Test
    fun `complete sensor can be shown`() {
        Assert.assertTrue(sensor().canBeShownInCar())
    }

    @Test
    fun `sensor without coordinates cannot be shown`() {
        Assert.assertFalse(sensor(latitude = null).canBeShownInCar())
        Assert.assertFalse(sensor(longitude = null).canBeShownInCar())
    }

    /**
     * Regression test: An empty title makes the car template builder throw, which
     * would take down the whole sensor list.
     */
    @Test
    fun `sensor without a device name cannot be shown`() {
        Assert.assertFalse(sensor(deviceName = "").canBeShownInCar())
        Assert.assertFalse(sensor(deviceName = "   ").canBeShownInCar())
    }
}
