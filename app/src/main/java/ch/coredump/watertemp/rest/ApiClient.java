package ch.coredump.watertemp.rest;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;

import ch.coredump.watertemp.rest.models.ApiError;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String API_URL = "https://watertemp-api.coredump.ch/api/";
    private ApiService apiService;

    public ApiClient() {
        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ss'.'SSS'Z'")
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();

        // Create retrofit REST adapter
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        // Create service
        apiService = retrofit.create(ApiService.class);
    }

    public ApiService getApiService() {
        return apiService;
    }

    public static ApiError parseError(Response<?> response) {
        final int statusCode = response.code();
        String body;
        try {
            body = response.errorBody().string().trim();
        } catch (IOException e) {
            e.printStackTrace();
            body = "Unknown error";
        }
        return new ApiError(statusCode, body);
    }
}