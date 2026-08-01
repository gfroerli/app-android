package ch.coredump.watertemp.rest

import ch.coredump.watertemp.rest.models.ApiSensor
import org.junit.Assert
import org.junit.Test
import java.time.ZoneOffset
import java.time.ZonedDateTime

class SensorRepositoryTest {
    private val now: ZonedDateTime = ZonedDateTime.of(2026, 7, 14, 12, 0, 0, 0, ZoneOffset.UTC)

    private fun sensor(id: Int, latestMeasurementAt: ZonedDateTime?) = ApiSensor(
        id = id,
        deviceName = "Sensor $id",
        caption = null,
        latitude = 47.2266,
        longitude = 8.8184,
        latestTemperature = 20.5f,
        latestMeasurementAt = latestMeasurementAt,
        sponsorId = null,
    )

    @Test
    fun `sensor without measurement is filtered out`() {
        val result = SensorRepository.filterFreshSensors(listOf(sensor(1, null)), now)
        Assert.assertTrue(result.isEmpty())
    }

    @Test
    fun `recent sensor is kept`() {
        val sensors = listOf(sensor(1, now.minusHours(2)), sensor(2, now.minusDays(2)))
        val result = SensorRepository.filterFreshSensors(sensors, now)
        Assert.assertEquals(listOf(1, 2), result.map { it.id })
    }

    @Test
    fun `outdated sensor is filtered out`() {
        val result = SensorRepository.filterFreshSensors(listOf(sensor(1, now.minusDays(4))), now)
        Assert.assertTrue(result.isEmpty())
    }

    @Test
    fun `sensor at exactly the max age is filtered out`() {
        val result = SensorRepository.filterFreshSensors(listOf(sensor(1, now.minusDays(3))), now)
        Assert.assertTrue(result.isEmpty())
    }

    @Test
    fun `empty list stays empty`() {
        val result = SensorRepository.filterFreshSensors(emptyList(), now)
        Assert.assertTrue(result.isEmpty())
    }
}
