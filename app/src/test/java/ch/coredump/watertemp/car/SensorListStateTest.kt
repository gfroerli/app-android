package ch.coredump.watertemp.car

import ch.coredump.watertemp.rest.models.ApiSensor
import org.junit.Assert
import org.junit.Test
import java.time.ZonedDateTime

class SensorListStateTest {
    private fun sensor(
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

    @Test
    fun `merge keeps the frozen order`() {
        val order = listOf(sensor(id = 3), sensor(id = 1), sensor(id = 2))
        // The reload returns the same sensors in a different order
        val loaded = listOf(sensor(id = 1), sensor(id = 2), sensor(id = 3))

        val merged = mergeIntoDisplayOrder(order, loaded)

        Assert.assertEquals(listOf(3, 1, 2), merged?.map { it.id })
    }

    @Test
    fun `merge drops sensors that are gone and appends new ones`() {
        val order = listOf(sensor(id = 3), sensor(id = 1))
        // Sensor 1 went stale, sensor 4 appeared
        val loaded = listOf(sensor(id = 4), sensor(id = 3))

        val merged = mergeIntoDisplayOrder(order, loaded)

        Assert.assertEquals(listOf(3, 4), merged?.map { it.id })
    }

    /**
     * Regression test: Appending every sensor to an empty order would show the whole
     * list in API order instead of by distance, until the next re-sort.
     */
    @Test
    fun `merge requires a rebuild if no shown sensor is left`() {
        val order = listOf(sensor(id = 3), sensor(id = 1))
        val loaded = listOf(sensor(id = 7), sensor(id = 8))

        Assert.assertNull(mergeIntoDisplayOrder(order, loaded))
        // The same applies to an order that was frozen while nothing was fresh
        Assert.assertNull(mergeIntoDisplayOrder(emptyList(), loaded))
    }

    @Test
    fun `merge requires a rebuild if nothing was loaded`() {
        val order = listOf(sensor(id = 3), sensor(id = 1))

        Assert.assertNull(mergeIntoDisplayOrder(order, emptyList()))
    }
}
