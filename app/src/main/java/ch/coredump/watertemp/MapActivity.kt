package ch.coredump.watertemp

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import ch.coredump.watertemp.rest.ApiClient
import ch.coredump.watertemp.rest.ApiService
import ch.coredump.watertemp.rest.SensorMeasurements
import ch.coredump.watertemp.rest.models.Measurement
import ch.coredump.watertemp.rest.models.Sensor
import com.mapbox.mapboxsdk.MapboxAccountManager
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import kotlinx.android.synthetic.main.activity_map.*
import kotlinx.android.synthetic.main.bottom_sheet_details.*
import kotlinx.android.synthetic.main.bottom_sheet_peek.*
import org.ocpsoft.prettytime.PrettyTime
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

const val TAG = "MapActivity"

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private var map: MapboxMap? = null
    private var apiService: ApiService? = null
    @SuppressLint("UseSparseArrays")
    private val sensors = HashMap<Int, SensorMeasurements>()
    private var activeMarker: Marker? = null
    private var bottomSheetBehavior: BottomSheetBehavior<*>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start mapbox account manager
        MapboxAccountManager.start(this.applicationContext, getString(R.string.mapbox_access_token))

        // Initialize the layout
        setContentView(R.layout.activity_map)

        // Create map view
        map_view.onCreate(savedInstanceState)

        // Initialize map
        map_view.getMapAsync(this)

        // Get API client
        // TODO: Use singleton dependency injection using something like dagger 2
        val apiClient = ApiClient(getString(R.string.public_api_token))
        apiService = apiClient.apiService

        // Initialize bottom sheet behavior
        this.bottomSheetBehavior = BottomSheetBehavior.from(details_bottom_sheet)

        // Set peek height as necessary
        this.bottomSheetBehavior!!.peekHeight = bottom_sheet_peek.height

        // Add bottom sheet listener
        this.bottomSheetBehavior!!.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                // Bottom sheet state changed
                Log.d(TAG, "State changed to " + newState)
                // TODO: If it's gone, make sure to deselect all markers.
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // Called repeatedly while bottom sheet slides up
            }
        })
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     */
    override fun onMapReady(mapboxMap: MapboxMap) {
        Log.d(TAG, "Map is ready")

        // Save map as attribute
        map = mapboxMap

        // Disable interactions that might confuse the user
        val settings = map!!.uiSettings
        settings.isRotateGesturesEnabled = false
        settings.isTiltGesturesEnabled = false
        settings.isCompassEnabled = false

        // Fetch sensors
        val sensorCall = apiService!!.listSensors()
        sensorCall.enqueue(onSensorsFetched())
    }

    private fun onSensorsFetched(): Callback<List<Sensor>> {
        return object : Callback<List<Sensor>> {
            override fun onResponse(call: Call<List<Sensor>>, response: Response<List<Sensor>>?) {
                if (response == null) {
                    Log.e(TAG, "Received null response from sensors endpoint")
                    return
                }
                Log.i(TAG, "Sensor response done!")
                if (response.isSuccessful) {
                    // Clear old sensor list
                    sensors.clear()

                    // Prepare list for sensor IDs
                    val idList = ArrayList<String>()

                    // Extract sensor information
                    for (sensor in response.body()!!) {
                        sensors.put(sensor.id, SensorMeasurements(sensor))
                        idList.add(sensor.id.toString())
                    }

                    // Fetch measurements
                    val ids = Utils.join(",", idList)
                    val measurementCall = apiService!!.listMeasurements(ids, idList.size * 5)
                    measurementCall.enqueue(onMeasurementsFetched())
                } else {
                    val error = ApiClient.parseError(response)
                    Log.e(TAG, error.toString())
                    Utils.showError(this@MapActivity, "Could not fetch sensors.\n" +
                            error.statusCode + ": " + error.message)
                }
            }

            override fun onFailure(call: Call<List<Sensor>>, t: Throwable) {
                val errmsg = "Fetching sensors failed: " + t.toString()
                Log.e(TAG, errmsg)
                Utils.showError(this@MapActivity, errmsg)
            }
        }
    }

    private fun onMeasurementsFetched(): Callback<List<Measurement>> {
        return object : Callback<List<Measurement>> {
            override fun onResponse(call: Call<List<Measurement>>, response: Response<List<Measurement>>?) {
                Log.i(TAG, "Measurement response done!")
                if (response != null) {
                    for (measurement in response.body()!!) {
                        Log.i(TAG, "Measurement: " + measurement.temperature)
                        if (sensors.containsKey(measurement.sensorId)) {
                            Log.e(TAG, "Sensor with id " + measurement.sensorId + " not found")
                            continue
                        }
                        sensors[measurement.sensorId]!!.addMeasurement(measurement)
                    }
                    updateMarkers()
                }
            }

            override fun onFailure(call: Call<List<Measurement>>, t: Throwable) {
                val errmsg = "Fetching measurements failed:" + t.toString()
                Log.e(TAG, errmsg)
                Utils.showError(this@MapActivity, errmsg)
            }
        }
    }

    private fun updateMarkers() {
        // Initialize icons
        val context = applicationContext
        val iconFactory = IconFactory.getInstance(context)
        // TODO: Proper tinting of blue marker like primary dark color
        val defaultIconDrawable = ContextCompat.getDrawable(context, R.drawable.blue_marker)
        // Default marker uses our accent color
        val activeIconDrawable = ContextCompat.getDrawable(context, R.drawable.default_marker)
        val defaultIcon = iconFactory.fromDrawable(defaultIconDrawable)
        val activeIcon = iconFactory.fromDrawable(activeIconDrawable)

        // Process sensors
        val locations = ArrayList<LatLng>()
        for (sensorMeasurement in sensors.values) {
            val sensor = sensorMeasurement.sensor
            val measurements = sensorMeasurement.measurements
            Log.i(TAG, "Add sensor" + sensor.caption)

            // Sort measurements by ID
            Collections.sort(measurements) { lhs, rhs ->
                val leftId = lhs.id
                val rightId = rhs.id
                leftId.compareTo(rightId)
            }

            // Create location object
            val lat = sensor.latitude
            val lng = sensor.longitude
            val location = LatLng(lat, lng)

            // Build caption
            val captionBuilder = StringBuilder()
            if (measurements.size > 0) {
                val measurement = measurements[measurements.size - 1]
                val pt = PrettyTime()
                captionBuilder.append(measurement.temperature)
                captionBuilder.append("°C (")
                captionBuilder.append(pt.format(measurement.createdAt))
                captionBuilder.append(")")
            } else {
                captionBuilder.append("No current measurement")
            }

            // Add the marker to the map
            map!!.addMarker(
                    MarkerOptions()
                            .position(LatLng(lat, lng))
                            .title(sensor.deviceName)
                            .snippet(captionBuilder.toString())
                            .icon(defaultIcon)
            )

            // Store location
            locations.add(location)
        }

        // Add marker click listener
        map!!.setOnMarkerClickListener { marker ->
            Log.d(TAG, "Marker ID: " + marker.id)

            // Update active marker icon
            if (this@MapActivity.activeMarker != null) {
                this@MapActivity.activeMarker!!.icon = defaultIcon
            }
            marker.icon = activeIcon
            this@MapActivity.activeMarker = marker

            // Update peek pane
            details_title.text = marker.title
            details_measurement.text = marker.snippet

            // Update detail pane
            details_sensor_caption.text = "TODO: Sensor details"

            // Show the details pane
            if (bottomSheetBehavior!!.state == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetBehavior!!.state = BottomSheetBehavior.STATE_EXPANDED
            }

            true
        }

        // Add map click listener
        map!!.setOnMapClickListener(MapboxMap.OnMapClickListener {
            Log.d(TAG, "Clicked on map")

            if (this@MapActivity.activeMarker == null) {
                return@OnMapClickListener
            }

            // No more active marker
            this@MapActivity.activeMarker!!.icon = defaultIcon
            this@MapActivity.activeMarker = null

            // Hide the details pane
            if (bottomSheetBehavior!!.state == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior!!.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        })

        // Change zoom to include all markers
        if (locations.size == 1) {
            val location = locations[0]
            map!!.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 13f))
        } else if (sensors.size > 1) {
            val boundingBoxBuilder = LatLngBounds.Builder()
            for (location in locations) {
                boundingBoxBuilder.include(location)
            }
            map!!.moveCamera(CameraUpdateFactory.newLatLngBounds(boundingBoxBuilder.build(), 100))
        }
    }

    // Lifecycle methods

    public override fun onResume() {
        super.onResume()
        map_view!!.onResume()
    }

    public override fun onPause() {
        super.onPause()
        map_view!!.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        map_view!!.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        map_view!!.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        map_view!!.onSaveInstanceState(outState!!)
    }

}