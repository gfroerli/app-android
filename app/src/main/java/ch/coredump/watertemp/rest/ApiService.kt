package ch.coredump.watertemp.rest

import ch.coredump.watertemp.rest.models.Measurement
import ch.coredump.watertemp.rest.models.Sensor
import ch.coredump.watertemp.rest.models.Sponsor
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Async API interface.
 */
interface ApiService {
    @GET("sensors")
    fun listSensors(): Call<List<Sensor>>

    @GET("measurements")
    fun listMeasurements(
            @Query("sensor_id") sensorId: Int,
            @Query("count") count: Int
    ): Call<List<Measurement>>

    @GET("measurements")
    fun listMeasurements(
            @Query("sensor_id") sensorIds: String,
            @Query("count") count: Int
    ): Call<List<Measurement>>

    @GET("sponsors")
    fun listSponsors(): Call<List<Sponsor>>
}