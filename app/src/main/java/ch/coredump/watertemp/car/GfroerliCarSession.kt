package ch.coredump.watertemp.car

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session
import ch.coredump.watertemp.BuildConfig
import ch.coredump.watertemp.rest.ApiClient
import ch.coredump.watertemp.rest.SensorRepository

/**
 * A single Android Auto session (one connection to a car host).
 */
class GfroerliCarSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        val apiClient = ApiClient(BuildConfig.GFROERLI_API_KEY_PUBLIC)
        return SensorListScreen(carContext, SensorRepository(apiClient.apiService))
    }
}
