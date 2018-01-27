package ch.coredump.watertemp.rest;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.threeten.bp.ZonedDateTime;

import java.io.IOException;

import ch.coredump.watertemp.BuildConfig;
import ch.coredump.watertemp.rest.models.ApiError;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String API_URL = "https://watertemp-api.coredump.ch/api/";
    private ApiService apiService;

    public ApiClient(final String authToken) {
        // Gson instance (for JSON (de)serialization)
        final Gson gson = new GsonBuilder()
                .setDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ss'.'SSS'Z'")
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapter(ZonedDateTime.class, GsonHelper.INSTANCE.getZDT_DESERIALIZER())
                .create();

        // Request interceptor (add authentication)
        final Interceptor authInterceptor = new Interceptor() {
            @Override
            public okhttp3.Response intercept(Chain chain) throws IOException {
                final Request original = chain.request();

                // Add request headers
                Request request = original.newBuilder()
                        .header("Authorization", "Bearer " + authToken)
                        .method(original.method(), original.body())
                        .build();

                return chain.proceed(request);
            }
        };

        // Request log interceptor
        final HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        if (BuildConfig.DEBUG) {
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);
        } else {
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.NONE);
        }

        // HTTP client
        final OkHttpClient httpClient = new OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor)
                .build();

        // Create retrofit REST adapter
        final Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(httpClient)
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