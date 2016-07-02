package ch.coredump.watertemp.rest;

import java.util.List;

import ch.coredump.watertemp.rest.models.Measurement;
import ch.coredump.watertemp.rest.models.Sensor;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Async API interface.
 */
public interface ApiService {
    @GET("sensors")
    Call<List<Sensor>> listSensors();
    @GET("measurements")
    Call<List<Measurement>> listMeasurements(@Query("sensor_id") int sensorId, @Query("count") int count);
    @GET("measurements")
    Call<List<Measurement>> listMeasurements(@Query("sensor_id") String sensorIds, @Query("count") int count);
}