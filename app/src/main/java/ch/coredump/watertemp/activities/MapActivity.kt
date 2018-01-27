package ch.coredump.watertemp.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.util.LongSparseArray
import android.util.SparseArray
import android.view.Menu
import android.view.MenuItem
import android.view.View
import ch.coredump.watertemp.R
import ch.coredump.watertemp.Utils
import ch.coredump.watertemp.rest.ApiClient
import ch.coredump.watertemp.rest.ApiService
import ch.coredump.watertemp.rest.SensorMeasurements
import ch.coredump.watertemp.rest.models.Measurement
import ch.coredump.watertemp.rest.models.Sensor
import ch.coredump.watertemp.rest.models.Sponsor
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.Icon
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
import org.threeten.bp.Instant
import org.threeten.bp.temporal.ChronoUnit
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    val TAG = "MapActivity"

    // The map instance
    private var map: MapboxMap? = null

    // Access the water-sensor service
    private var apiService: ApiService? = null

    // Mapping from sensor IDs to `SensorMeasurements` instances
    @SuppressLint("UseSparseArrays")
    private val sensors = HashMap<Int, SensorMeasurements>()

    // Mapping from sponsor IDs to `Sponsor` instances
    private val sponsors = SparseArray<Sponsor>()

    // Mapping from map marker IDs to sensor IDs
    private val sensorMarkers = LongSparseArray<Int>()

    // The currently active marker
    private var activeMarker: Marker? = null

    // Class to control how the bottom sheet behaves
    private var bottomSheetBehavior: BottomSheetBehavior<*>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize mapbox
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))

        // Initialize the layout
        setContentView(R.layout.activity_map)

        // Initialize the action bar
        setSupportActionBar(main_action_bar)

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

    override fun onStart() {
        super.onStart()
        map_view.onStart()
    }

    override fun onStop() {
        super.onStop()
        map_view.onStop()
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

        this.fetchData()
    }

    /**
     * Request sensors and sponsors.
     */
    private fun fetchData() {
        Log.d(TAG, "Fetching data from API")

        // Fetch sensors
        val sensorCall = apiService!!.listSensors()
        sensorCall.enqueue(this.onSensorsFetched())

        // Fetch sponsors
        val sponsorCall = apiService!!.listSponsors()
        sponsorCall.enqueue(this.onSponsorsFetched())
    }

    private fun onSensorsFetched(): Callback<List<Sensor>> {
        return object : Callback<List<Sensor>> {
            override fun onResponse(call: Call<List<Sensor>>, response: Response<List<Sensor>>?) {
                // Handle null response
                if (response == null) {
                    Log.e(TAG, "Received null response from sensors endpoint")
                    return
                }

                // Handle unsuccessful response
                if (!response.isSuccessful) {
                    val error = ApiClient.parseError(response)
                    Log.e(TAG, error.toString())
                    Utils.showError(this@MapActivity, "Could not fetch sensors.\n" +
                            error.statusCode + ": " + error.message)
                }

                // Success!
                Log.d(TAG, "Sensors response successful")

                // Clear old sensor list
                sensors.clear()

                // Prepare list for sensor IDs
                val idList = ArrayList<String>()

                // Extract sensor information
                for (sensor in response.body()!!) {
                    sensors.put(sensor.id, SensorMeasurements(sensor))
                    idList.add(sensor.id.toString())
                }

                updateMarkers()

                // Fetch measurements
                // TODO: Fetch aggregations instead
//                val ids = Utils.join(",", idList)
//                val measurementCall = apiService!!.listMeasurements(ids, idList.size * 5)
//                measurementCall.enqueue(onMeasurementsFetched())
            }

            override fun onFailure(call: Call<List<Sensor>>, t: Throwable) {
                val errmsg = "Fetching sensors failed: " + t.toString()
                Log.e(TAG, errmsg)
                Utils.showError(this@MapActivity, errmsg)
            }
        }
    }

    private fun onSponsorsFetched(): Callback<List<Sponsor>> {
        return object : Callback<List<Sponsor>> {
            override fun onResponse(call: Call<List<Sponsor>>, response: Response<List<Sponsor>>?) {
                // Handle null response
                if (response == null) {
                    Log.e(TAG, "Received null response from sponsors endpoint")
                    return
                }

                // Handle unsuccessful response
                if (!response.isSuccessful) {
                    Log.e(TAG, "Sponsors response not successful")
                    val error = ApiClient.parseError(response)
                    Log.e(TAG, error.toString())
                    Utils.showError(this@MapActivity, "Could not fetch sensors.\n" +
                            error.statusCode + ": " + error.message)
                    return
                }

                // Success!
                Log.d(TAG, "Sponsors response successful")

                // Store sponsors
                sponsors.clear()
                for (sponsor in response.body()!!) {
                    sponsors.put(sponsor.id, sponsor)
                }
            }

            override fun onFailure(call: Call<List<Sponsor>>, t: Throwable) {
                val errmsg = "Fetching sponsors failed: " + t.toString()
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
                        if (!sensors.containsKey(measurement.sensorId)) {
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

    /**
     * Show the bottom sheet if it isn't already visible.
     */
    private fun showBottomSheet() {
        if (bottomSheetBehavior!!.state == BottomSheetBehavior.STATE_COLLAPSED) {
            bottomSheetBehavior!!.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    /**
     * Hide the bottom sheet if it's visible.
     */
    private fun hideBottomSheet() {
        if (bottomSheetBehavior!!.state == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior!!.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    private fun updateMarkers() {
        // Initialize icons
        val context = applicationContext
        val iconFactory = IconFactory.getInstance(context)
        val defaultIcon = iconFactory.fromResource(R.drawable.blue_marker)
        val activeIcon = iconFactory.fromResource(R.drawable.mapbox_marker_icon_default)

        // Process sensors
        val locations = ArrayList<LatLng>()
        for (sensorMeasurement in sensors.values) {
            val sensor = sensorMeasurement.sensor
            val measurements = sensorMeasurement.measurements
            Log.i(TAG, "Add sensor " + sensor.deviceName)

            // Sort measurements by ID
            Collections.sort(measurements) { lhs, rhs ->
                val leftId = lhs.id
                val rightId = rhs.id
                leftId.compareTo(rightId)
            }

            // Create location object
            val lat = sensor.latitude
            val lng = sensor.longitude
            if (lat == null || lng == null) {
                Log.w(TAG, "Skipping sensor without location: " + sensor.deviceName)
                continue
            }
            val location = LatLng(lat, lng)

            // Add the marker to the map
            val marker = map!!.addMarker(
                    MarkerOptions()
                            .position(LatLng(lat, lng))
                            .title(sensor.deviceName)
                            .icon(defaultIcon)
            )

            // Create a mapping from the marker id to the sensor id
            sensorMarkers.put(marker.id, sensor.id)

            // Store location
            locations.add(location)
        }

        // Add marker click listener
        map!!.setOnMarkerClickListener { marker ->
            this@MapActivity.onMarkerSelected(marker, defaultIcon, activeIcon)
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
            this.hideBottomSheet()
        })

        // Change zoom to include all markers
        if (locations.size == 1) {
            val location = locations[0]
            map!!.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 13.0))
        } else if (sensors.size > 1) {
            val boundingBoxBuilder = LatLngBounds.Builder()
            for (location in locations) {
                boundingBoxBuilder.include(location)
            }
            map!!.moveCamera(CameraUpdateFactory.newLatLngBounds(boundingBoxBuilder.build(), 100))
        }
    }

    /**
     * Called when a marker is selected.
     */
    private fun onMarkerSelected(marker: Marker, defaultIcon: Icon, activeIcon: Icon): Boolean {
        Log.d(TAG, "Selected marker ID: " + marker.id)

        // Update active marker icon
        if (this.activeMarker != null) {
            this.activeMarker!!.icon = defaultIcon
        }
        marker.icon = activeIcon
        this.activeMarker = marker

        // Fetch sensor for that marker
        val sensorId: Int? = sensorMarkers[marker.id]
        val sensorMeasurements = sensors[sensorId!!]
        if (sensorMeasurements == null) {
            Log.e(TAG, "Sensor with id $sensorId not found")
            Utils.showError(this, "Sensor not found")
            return true
        }
        val sensor = sensorMeasurements.sensor
        val measurements = sensorMeasurements.measurements

        // Fetch sensor measurements from last three days
        val since = Instant.now().minus(3, ChronoUnit.DAYS)
        val measurementCall = apiService!!.listMeasurementsSince(sensor.id, since)
        measurementCall.enqueue(onMeasurementsFetched())

        // Lookup sponsor for that sensor
        val sponsor: Sponsor? = sensor.sponsorId?.let { sponsors.get(it) }

        // Get last temperature measurement
        val captionBuilder = StringBuilder()
        if (sensor.lastMeasurement != null) {
            val pt = PrettyTime()
            captionBuilder.append(String.format("%.2f", sensor.lastMeasurement.temperature))
            captionBuilder.append("°C (")
            val createdAtDate = Date(sensor.lastMeasurement.createdAt.toInstant().toEpochMilli())
            captionBuilder.append(pt.format(createdAtDate))
            captionBuilder.append(")")
        } else {
            captionBuilder.append(getString(R.string.no_measurement))
        }

        // Update peek pane
        details_title.text = sensor.deviceName
        details_measurement.text = captionBuilder.toString()
        if (sensor.caption.isNullOrBlank()) {
            details_caption.visibility = View.GONE
        } else {
            details_caption.text = sensor.caption
            details_caption.visibility = View.VISIBLE
        }

        // Update details section
        details_sensor_caption.text = "TODO: Sensor details"

        // Update sponsor section
        if (sponsor == null) {
            details_section_sponsor.visibility = View.GONE
        } else {
            details_sponsor_section_header.text = getString(R.string.section_header_sponsor, sponsor.name)
            val sponsorDescriptionBuilder = StringBuilder()
            sponsorDescriptionBuilder.append(getString(R.string.sponsor_description, sponsor.name))
            sponsorDescriptionBuilder.append("\n")
            sponsorDescriptionBuilder.append(sponsor.description)
            details_sponsor_description.text = sponsorDescriptionBuilder.toString()
            details_section_sponsor.visibility = View.VISIBLE
        }

        // Show the details pane
        this.showBottomSheet()

        return true
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

    // Menu

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_about -> {
                Log.d(TAG, "Menu: About")
                val intent = Intent(this, AboutActivity::class.java)
                startActivity(intent)
            }
            R.id.action_refresh -> {
                Log.d(TAG, "Menu: Refresh")
                fetchData()
            }
            else -> Log.w(TAG, "Selected unknown menu entry: " + item)
        }
        return super.onOptionsItemSelected(item)
    }

    // Key events

    override fun onBackPressed() {
        // If the bottom sheet is visible, close it on back button press.
        // Otherwise, fall back to default behavior.
        if (bottomSheetBehavior!!.state == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior!!.state = BottomSheetBehavior.STATE_COLLAPSED
        } else {
            super.onBackPressed()
        }
    }
}