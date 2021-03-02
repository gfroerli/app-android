package ch.coredump.watertemp.rest

import ch.coredump.watertemp.rest.models.Measurement
import ch.coredump.watertemp.rest.models.Sensor
import ch.coredump.watertemp.rest.models.SensorDetails
import ch.coredump.watertemp.rest.models.Sponsor
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
    fun listSensors(): Call<List<Sensor>>

    @GET("mobile_app/sensors/{id}")
    fun getSensorDetails(
            @Path("id") sensorId: Int,
    ): Call<SensorDetails>

    @GET("mobile_app/sensors/{id}/sponsor")
    fun getSponsor(
            @Path("id") sensorId: Int,
    ): Call<Sponsor>

    // Old API endpoints, deprecated!

    @GET("measurements")
    fun listMeasurementsSince(
            @Query("sensor_id") sensorId: Int,
            @Query("created_after") createdAfter: Instant
    ): Call<List<Measurement>>
}