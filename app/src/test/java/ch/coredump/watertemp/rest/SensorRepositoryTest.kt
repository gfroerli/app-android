package ch.coredump.watertemp.rest

import ch.coredump.watertemp.rest.models.ApiSensor
import ch.coredump.watertemp.rest.models.ApiSponsor
import ch.coredump.watertemp.rest.models.SponsorType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert
import org.junit.Test
import retrofit2.Call
import retrofit2.Response
import retrofit2.mock.Calls
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

class SensorRepositoryTest {
    private val now: ZonedDateTime = ZonedDateTime.of(2026, 7, 14, 12, 0, 0, 0, ZoneOffset.UTC)

    private fun sensor(id: Int, latestMeasurementAt: ZonedDateTime?, sponsorId: Int? = null) = ApiSensor(
        id = id,
        deviceName = "Sensor $id",
        caption = null,
        latitude = 47.2266,
        longitude = 8.8184,
        latestTemperature = 20.5f,
        latestMeasurementAt = latestMeasurementAt,
        sponsorId = sponsorId,
    )

    private fun sponsor() = ApiSponsor(
        id = 1,
        name = "Coredump",
        description = null,
        logoUrl = null,
        sponsorType = SponsorType.Sponsor,
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

    /**
     * Load the sponsor from a faked API response. The fake call invokes the callback
     * synchronously, so the result is available once this returns.
     */
    private fun loadSponsor(response: Response<ApiSponsor>): Result<ApiSponsor> {
        var result: Result<ApiSponsor>? = null
        val sensor = sensor(1, now, sponsorId = 9)
        SensorRepository(FakeApiService(Calls.response(response))).loadSponsor(sensor) {
            result = it
        }
        return checkNotNull(result) { "The callback was not invoked" }
    }

    /** Regression test for NPE on empty success body. */
    @Test
    fun `successful response without a body fails instead of crashing`() {
        // A successful response has no error body, so parsing one would throw. This
        // happens for an empty "204 No Content" response, or a JSON body of "null".
        val result = loadSponsor(Response.success(null))

        Assert.assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        Assert.assertTrue("Expected an ApiException, got $exception", exception is ApiException)
        Assert.assertEquals(200, (exception as ApiException).statusCode)
    }

    @Test
    fun `error response is reported with its status code`() {
        val result = loadSponsor(Response.error(404, "not found".toResponseBody()))

        Assert.assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        Assert.assertTrue("Expected an ApiException, got $exception", exception is ApiException)
        Assert.assertEquals(404, (exception as ApiException).statusCode)
    }

    @Test
    fun `sponsor is returned on success`() {
        val result = loadSponsor(Response.success(sponsor()))

        Assert.assertEquals("Coredump", result.getOrNull()?.name)
    }

    @Test
    fun `sponsor is queried with the sensor id, not the sponsor id`() {
        val service = FakeApiService(Calls.response(Response.success(sponsor())))

        SensorRepository(service).loadSponsor(sensor(7, now, sponsorId = 42)) {}

        Assert.assertEquals(listOf(7), service.requestedSensorIds)
    }

    @Test
    fun `sensor without a sponsor is not requested`() {
        val service = FakeApiService(Calls.response(Response.success(sponsor())))
        var called = false

        SensorRepository(service).loadSponsor(sensor(1, now, sponsorId = null)) { called = true }

        Assert.assertTrue(service.requestedSensorIds.isEmpty())
        Assert.assertFalse("The callback should not be invoked", called)
    }
}

/**
 * An [ApiService] that returns a canned sponsor call and records the IDs it was queried
 * with. The other endpoints are not used by these tests.
 */
private class FakeApiService(private val call: Call<ApiSponsor>) : ApiService {
    val requestedSensorIds = mutableListOf<Int>()

    override fun getSponsor(sensorId: Int): Call<ApiSponsor> {
        requestedSensorIds += sensorId
        return call
    }

    override fun listSensors() = TODO()
    override fun getSensorDetails(sensorId: Int) = TODO()
    override fun listMeasurementsSince(sensorId: Int, createdAfter: Instant) = TODO()
}
