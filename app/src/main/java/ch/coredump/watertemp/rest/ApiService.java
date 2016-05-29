package ch.coredump.watertemp.rest;

import java.util.List;

import ch.coredump.watertemp.rest.models.Sensor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.http.GET;

/**
 * Async API interface.
 */
public interface ApiService {
    @GET("sensors")
    Call<List<Sensor>> listSensors();
}