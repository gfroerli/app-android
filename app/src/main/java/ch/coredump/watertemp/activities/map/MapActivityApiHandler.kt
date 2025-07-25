package ch.coredump.watertemp.activities.map

import android.util.Log
import androidx.compose.material.ExperimentalMaterialApi
import ch.coredump.watertemp.Config
import ch.coredump.watertemp.R
import ch.coredump.watertemp.Utils
import ch.coredump.watertemp.rest.ApiClient
import ch.coredump.watertemp.rest.models.ApiMeasurement
import ch.coredump.watertemp.rest.models.ApiSensor
import ch.coredump.watertemp.rest.models.ApiSensorDetails
import ch.coredump.watertemp.rest.models.ApiSponsor
import ch.coredump.watertemp.ui.viewmodels.Measurement
import ch.coredump.watertemp.ui.viewmodels.SensorBottomSheetViewModel
import ch.coredump.watertemp.utils.ProgressCounter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * Handles API response callbacks for the MapActivity
 */
@ExperimentalMaterialApi
class MapActivityApiHandler(
    private val activity: MapActivity,
    private val progressCounter: ProgressCounter,
    private val mapData: MapData,
    private val bottomSheetViewModel: SensorBottomSheetViewModel,
    private val updateMapMarkers: () -> Unit
) {

    companion object {
        private const val TAG = "MapActivityApiHandler"
    }

    fun onSensorsFetched(): Callback<List<ApiSensor>> {
        return object : Callback<List<ApiSensor>> {
            override fun onResponse(call: Call<List<ApiSensor>>, response: Response<List<ApiSensor>>) {
                progressCounter.decrement()

                // Handle unsuccessful response
                if (!response.isSuccessful) {
                    val error = ApiClient.parseError(response)
                    Log.e(TAG, "Could not fetch sensors (HTTP " + error.statusCode + "): " + error.message)
                    val message = activity.getString(
                        R.string.fetching_sensor_data_failed,
                        error.statusCode,
                        Config.SUPPORT_EMAIL
                    )
                    Utils.showError(activity, message)
                    return
                }

                // Success!
                Log.d(TAG, "Sensors response successful")

                // Ensure bottom sheet is hidden and clear old sensor data
                // TODO(#6): Don't hide, but update data!
                bottomSheetViewModel.hideBottomSheet()
                bottomSheetViewModel.clearData()
                mapData.clearSensors()

                // Prepare list for sensor IDs
                val idList = ArrayList<String>()

                // Extract sensor information
                val now = ZonedDateTime.now()
                for (sensor in response.body()!!) {
                    if (sensor.latestMeasurementAt != null && ChronoUnit.DAYS.between(sensor.latestMeasurementAt, now) < 3) {
                        Log.d(TAG, "Adding sensor " + sensor.id)
                        mapData.addSensor(sensor)
                        idList.add(sensor.id.toString())
                    } else {
                        Log.d(TAG, "Ignoring sensor " + sensor.id + " (missing or outdated measurement)")
                    }
                }

                updateMapMarkers()

                // Fetch measurements
                // TODO: Fetch aggregations instead
//                val ids = Utils.join(",", idList)
//                val measurementCall = apiService!!.listMeasurements(ids, idList.size * 5)
//                measurementCall.enqueue(onMeasurementsFetched())
            }

            override fun onFailure(call: Call<List<ApiSensor>>, t: Throwable) {
                progressCounter.decrement()
                Log.e(TAG, "Fetching sensors failed: $t")
                val message = activity.getString(
                    R.string.fetching_data_failed,
                    activity.getString(R.string.data_sensors)
                )
                Utils.showError(activity, message)
            }
        }
    }

    fun onSensorDetailsFetched(): Callback<ApiSensorDetails> {
        return object : Callback<ApiSensorDetails> {
            override fun onResponse(call: Call<ApiSensorDetails>, response: Response<ApiSensorDetails>) {
                progressCounter.decrement()

                Log.i(TAG, "Processing sensor details response")
                response.body()?.let { details ->
                    bottomSheetViewModel.addDetails(details)
                }
            }

            override fun onFailure(call: Call<ApiSensorDetails>, t: Throwable) {
                progressCounter.decrement()

                Log.e(TAG, "Fetching sensor details failed: $t")
                val message = activity.getString(
                    R.string.fetching_data_failed,
                    activity.getString(R.string.data_sensor_details)
                )
                Utils.showError(activity, message)
            }
        }
    }

    fun onSponsorFetched(): Callback<ApiSponsor> {
        return object : Callback<ApiSponsor> {
            override fun onResponse(call: Call<ApiSponsor>, response: Response<ApiSponsor>) {
                progressCounter.decrement()

                Log.i(TAG, "Processing sponsor response")
                response.body()?.let { sponsor ->
                    mapData.addSponsor(sponsor)
                    bottomSheetViewModel.addSponsor(sponsor)
                }
            }

            override fun onFailure(call: Call<ApiSponsor>, t: Throwable) {
                progressCounter.decrement()

                Log.e(TAG, "Fetching sponsor failed: $t")
                val message = activity.getString(
                    R.string.fetching_data_failed,
                    activity.getString(R.string.data_sponsor)
                )
                Utils.showError(activity, message)
            }
        }
    }

    fun onMeasurementsFetched(): Callback<List<ApiMeasurement>> {
        return object : Callback<List<ApiMeasurement>> {
            override fun onResponse(call: Call<List<ApiMeasurement>>, response: Response<List<ApiMeasurement>>) {
                progressCounter.decrement()

                Log.i(TAG, "Processing measurements response")
                response.body()?.let {
                    bottomSheetViewModel.setMeasurements(
                        it.map(Measurement::fromApiMeasurement)
                    )
                }
            }

            override fun onFailure(call: Call<List<ApiMeasurement>>, t: Throwable) {
                progressCounter.decrement()

                Log.e(TAG, "Fetching measurements failed: $t")
                val message = activity.getString(
                    R.string.fetching_data_failed,
                    activity.getString(R.string.data_measurements)
                )
                Utils.showError(activity, message)
            }
        }
    }
}
