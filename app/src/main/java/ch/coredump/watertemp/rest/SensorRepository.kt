package ch.coredump.watertemp.rest

import ch.coredump.watertemp.Config
import ch.coredump.watertemp.rest.models.ApiSensor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * Thrown when the API returns an unsuccessful HTTP response.
 */
class ApiException(val statusCode: Int, message: String) : Exception(message)

/**
 * Provides access to sensor data from the Gfrörli API.
 */
class SensorRepository(private val apiService: ApiService) {

    /**
     * Fetch all sensors with a fresh measurement (see [filterFreshSensors]).
     *
     * The [onResult] callback is invoked on the main thread.
     */
    fun loadFreshSensors(onResult: (Result<List<ApiSensor>>) -> Unit) {
        apiService.listSensors().enqueue(object : Callback<List<ApiSensor>> {
            override fun onResponse(call: Call<List<ApiSensor>>, response: Response<List<ApiSensor>>) {
                if (!response.isSuccessful) {
                    val error = ApiClient.parseError(response)
                    onResult(Result.failure(ApiException(error.statusCode, error.message)))
                    return
                }
                val sensors = response.body() ?: emptyList()
                onResult(Result.success(filterFreshSensors(sensors, ZonedDateTime.now())))
            }

            override fun onFailure(call: Call<List<ApiSensor>>, t: Throwable) {
                onResult(Result.failure(t))
            }
        })
    }

    companion object {
        /**
         * Return only the sensors with a measurement within the last
         * [Config.SENSOR_MAX_AGE_DAYS] days.
         */
        fun filterFreshSensors(sensors: List<ApiSensor>, now: ZonedDateTime): List<ApiSensor> =
            sensors.filter {
                it.latestMeasurementAt != null &&
                    ChronoUnit.DAYS.between(it.latestMeasurementAt, now) < Config.SENSOR_MAX_AGE_DAYS
            }
    }
}
