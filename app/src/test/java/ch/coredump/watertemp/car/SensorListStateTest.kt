package ch.coredump.watertemp.car

import ch.coredump.watertemp.rest.models.ApiSensor
import org.junit.Assert
import org.junit.Test
import java.time.ZonedDateTime

class SensorListStateTest {
    private fun apiSensor(
        id: Int = 1,
        deviceName: String = "Zürichsee",
        latitude: Double? = 47.2266,
        longitude: Double? = 8.8184,
    ) = ApiSensor(
        id = id,
        deviceName = deviceName,
        caption = null,
        latitude = latitude,
        longitude = longitude,
        latestTemperature = 20.5f,
        latestMeasurementAt = ZonedDateTime.now(),
        sponsorId = null,
    )

    /** A sensor that can be shown, for the merge tests below. */
    private fun sensor(id: Int) = CarSensor.of(apiSensor(id = id))!!

    @Test
    fun `complete sensor can be shown`() {
        val carSensor = CarSensor.of(apiSensor())

        Assert.assertNotNull(carSensor)
        Assert.assertEquals(47.2266, carSensor!!.latitude, 0.0)
        Assert.assertEquals(8.8184, carSensor.longitude, 0.0)
        Assert.assertEquals("Zürichsee", carSensor.deviceName)
    }

    @Test
    fun `sensor without coordinates cannot be shown`() {
        Assert.assertNull(CarSensor.of(apiSensor(latitude = null)))
        Assert.assertNull(CarSensor.of(apiSensor(longitude = null)))
    }

    /**
     * Regression test: An empty title makes the car template builder throw, which
     * would take down the whole sensor list.
     */
    @Test
    fun `sensor without a device name cannot be shown`() {
        Assert.assertNull(CarSensor.of(apiSensor(deviceName = "")))
        Assert.assertNull(CarSensor.of(apiSensor(deviceName = "   ")))
    }

    @Test
    fun `merge keeps the frozen order`() {
        val order = listOf(sensor(3), sensor(1), sensor(2))
        // The reload returns the same sensors in a different order
        val loaded = listOf(sensor(1), sensor(2), sensor(3))

        val merged = mergeIntoDisplayOrder(order, loaded)

        Assert.assertEquals(listOf(3, 1, 2), merged?.map { it.id })
    }

    @Test
    fun `merge drops sensors that are gone and appends new ones`() {
        val order = listOf(sensor(3), sensor(1))
        // Sensor 1 went stale, sensor 4 appeared
        val loaded = listOf(sensor(4), sensor(3))

        val merged = mergeIntoDisplayOrder(order, loaded)

        Assert.assertEquals(listOf(3, 4), merged?.map { it.id })
    }

    /**
     * Regression test: Appending every sensor to an empty order would show the whole
     * list in API order instead of by distance, until the next re-sort.
     */
    @Test
    fun `merge requires a rebuild if no shown sensor is left`() {
        val order = listOf(sensor(3), sensor(1))
        val loaded = listOf(sensor(7), sensor(8))

        Assert.assertNull(mergeIntoDisplayOrder(order, loaded))
        // The same applies to an order that was frozen while nothing was fresh
        Assert.assertNull(mergeIntoDisplayOrder(emptyList(), loaded))
    }

    @Test
    fun `merge requires a rebuild if nothing was loaded`() {
        val order = listOf(sensor(3), sensor(1))

        Assert.assertNull(mergeIntoDisplayOrder(order, emptyList()))
    }
}
