package ch.coredump.watertemp.rest

import ch.coredump.watertemp.rest.models.ApiMeasurement
import ch.coredump.watertemp.rest.models.ApiSensor
import ch.coredump.watertemp.rest.models.ApiSensorDetails
import ch.coredump.watertemp.rest.models.ApiSponsor
import org.threeten.bp.Instant
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Async API interface.
 */
interface ApiService {
    @GET("mobile_app/sensors")
    fun listSensors(): Call<List<ApiSensor>>

    @GET("mobile_app/sensors/{id}")
    fun getSensorDetails(
            @Path("id") sensorId: Int,
    ): Call<ApiSensorDetails>

    @GET("mobile_app/sensors/{id}/sponsor")
    fun getSponsor(
            @Path("id") sensorId: Int,
    ): Call<ApiSponsor>

    // Old API endpoints, deprecated!

    @GET("measurements")
    fun listMeasurementsSince(
            @Query("sensor_id") sensorId: Int,
            @Query("created_after") createdAfter: Instant
    ): Call<List<ApiMeasurement>>
}