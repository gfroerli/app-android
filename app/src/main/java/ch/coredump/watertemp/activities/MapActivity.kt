package ch.coredump.watertemp.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.SparseArray
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import ch.coredump.watertemp.BuildConfig
import ch.coredump.watertemp.R
import ch.coredump.watertemp.Utils
import ch.coredump.watertemp.rest.ApiClient
import ch.coredump.watertemp.rest.ApiService
import ch.coredump.watertemp.rest.SensorMeasurements
import ch.coredump.watertemp.rest.models.Measurement
import ch.coredump.watertemp.rest.models.Sensor
import ch.coredump.watertemp.rest.models.SensorDetails
import ch.coredump.watertemp.rest.models.Sponsor
import ch.coredump.watertemp.utils.ProgressCounter
import com.bumptech.glide.Glide
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.Symbol
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import kotlinx.android.synthetic.main.activity_map.*
import kotlinx.android.synthetic.main.bottom_sheet_details.*
import kotlinx.android.synthetic.main.bottom_sheet_peek.*
import org.ocpsoft.prettytime.PrettyTime
import org.threeten.bp.Duration
import org.threeten.bp.Instant
import org.threeten.bp.temporal.ChronoUnit
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*


// Marker image names
private const val MARKER_DEFAULT = "marker_default"
private const val MARKER_ACTIVE = "marker_active"

private const val TAG = "MapActivity"

class MapActivity : AppCompatActivity(), OnMapReadyCallback {
    // The map instance
    private var map: MapboxMap? = null
    private var symbolManager: SymbolManager? = null

    // Access the water-sensor service
    private var apiService: ApiService? = null

    // Mapping from sensor IDs to `SensorMeasurements` instances
    @SuppressLint("UseSparseArrays")
    private val sensors = HashMap<Int, SensorMeasurements>()

    // Mapping from sponsor IDs to `Sponsor` instances
    private val sponsors = SparseArray<Sponsor>()

    // The currently active marker
    private var activeMarker: Symbol? = null

    // Class to control how the bottom sheet behaves
    private var bottomSheetBehavior: BottomSheetBehavior<*>? = null

    // Views
    private var chart3days: LineChart? = null

    // Activity indicator
    private var progressCounter: ProgressCounter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize mapbox
        Mapbox.getInstance(this, BuildConfig.MAPBOX_ACCESS_TOKEN)

        // Initialize the layout
        setContentView(R.layout.activity_map)

        // Initialize the action bar
        setSupportActionBar(main_action_bar)
        supportActionBar!!.title = getString(R.string.activity_map)

        // Progress counter
        this.progressCounter = ProgressCounter(findViewById(R.id.loadingbar))

        // Create map view
        this.map_view.onCreate(savedInstanceState)

        // Initialize map
        this.map_view.getMapAsync(this)

        // Get API client
        // TODO: Use singleton dependency injection using something like dagger 2
        val apiClient = ApiClient(BuildConfig.GFROERLI_API_KEY_PUBLIC)
        apiService = apiClient.apiService

        // Initialize bottom sheet behavior
        this.bottomSheetBehavior = BottomSheetBehavior.from(details_bottom_sheet)

        // Initially hidden
        this.bottomSheetBehavior!!.state = BottomSheetBehavior.STATE_HIDDEN

        // We want two sizes: Peek height and full height
        this.bottomSheetBehavior!!.isFitToContents = false

        // Add bottom sheet listener
        this.bottomSheetBehavior!!.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                // Bottom sheet state changed
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    // Clear chart data
                    this@MapActivity.chart3days!!.clear()

                    // Deselect all markers
                    this@MapActivity.deselectMarkers()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // Called repeatedly while bottom sheet slides up
            }
        })

        // Style charts
        this.chart3days = findViewById(R.id.chart_3days)
        this.chart3days!!.setNoDataText(getString(R.string.chart_no_data))
        this.chart3days!!.setDrawGridBackground(false)
        this.chart3days!!.setDrawBorders(false)
        this.chart3days!!.description.isEnabled = false
        this.chart3days!!.xAxis.isEnabled = false
        this.chart3days!!.axisRight.isEnabled = false
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

        mapboxMap.setStyle(Style.OUTDOORS) { style ->
            Log.d(TAG, "Style loaded")

            // Load marker icon
            style.addImage(MARKER_DEFAULT, ContextCompat.getDrawable(this, R.drawable.blue_marker)!!)
            style.addImage(MARKER_ACTIVE, ContextCompat.getDrawable(this, R.drawable.mapbox_marker_icon_default)!!)

            // Initialize symbol manager
            this.symbolManager = SymbolManager(this.map_view, mapboxMap, style)

            // Save map as attribute
            this.map = mapboxMap

            // Disable interactions that might confuse the user
            val settings = map!!.uiSettings
            settings.isRotateGesturesEnabled = false
            settings.isTiltGesturesEnabled = false
            settings.isCompassEnabled = false

            this.fetchInitialData()
        }
    }

    /**
     * Request initial data.
     */
    private fun fetchInitialData() {
        Log.d(TAG, "Fetching initial data from API")

        // Fetch sensors
        val sensorCall = apiService!!.listSensors()
        this.progressCounter!!.increment()
        sensorCall.enqueue(this.onSensorsFetched())

        // TODO: Do we need to re-fetch sensor-details of currently showing sensor?
    }

    private fun onSensorsFetched(): Callback<List<Sensor>> {
        return object : Callback<List<Sensor>> {
            override fun onResponse(call: Call<List<Sensor>>, response: Response<List<Sensor>>?) {
                this@MapActivity.progressCounter!!.decrement()

                // Handle null response
                if (response == null) {
                    Log.e(TAG, "Received null response from sensors endpoint")
                    return
                }

                // Handle unsuccessful response
                if (!response.isSuccessful) {
                    val error = ApiClient.parseError(response)
                    Log.e(TAG, error.toString())
                    Utils.showError(
                        this@MapActivity,
                        "Could not fetch sensors.\n ${error.statusCode}: ${error.message}"
                    )
                }

                // Success!
                Log.d(TAG, "Sensors response successful")

                // Clear old sensor list
                sensors.clear()

                // Prepare list for sensor IDs
                val idList = ArrayList<String>()

                // Extract sensor information
                for (sensor in response.body()!!) {
                    sensors[sensor.id] = SensorMeasurements(sensor)
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
                this@MapActivity.progressCounter!!.decrement()
                Log.e(TAG, "Fetching sensors failed: $t")
                Utils.showError(
                    this@MapActivity,
                    getString(R.string.fetching_data_failed, getString(R.string.data_sensors)
                )
                )
            }
        }
    }

    /**
     * Show the bottom sheet if it isn't already visible.
     */
    private fun showBottomSheet() {
        bottomSheetBehavior?.let {
            if (it.state == BottomSheetBehavior.STATE_HIDDEN) {
                it.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }
    }

    /**
     * Hide the bottom sheet if it's visible.
     */
    private fun hideBottomSheet() {
        bottomSheetBehavior?.let {
            if (it.state != BottomSheetBehavior.STATE_HIDDEN) {
                it.state = BottomSheetBehavior.STATE_HIDDEN
            }
        }
    }

    private fun updateMarkers() {
        // Clear old markers
        this.symbolManager!!.deleteAll()

        // Process sensors
        val locations = ArrayList<LatLng>()
        for (sensorMeasurement in sensors.values) {
            val sensor = sensorMeasurement.sensor
            val measurements = sensorMeasurement.measurements
            Log.i(TAG, "Add sensor ${sensor.deviceName} (id=${sensor.id})")

            // Sort measurements by ID
            // See https://github.com/gfroerli/gfroerli-api/issues/40
            measurements.sortWith { lhs, rhs ->
                val leftId = lhs.id
                val rightId = rhs.id
                leftId.compareTo(rightId)
            }

            // Create location object
            val lat = sensor.latitude
            val lng = sensor.longitude
            if (lat == null || lng == null) {
                Log.w(TAG, "Skipping sensor without location: ${sensor.deviceName}")
                continue
            }
            val location = LatLng(lat, lng)

            // Create marker on map
            val marker = this.symbolManager!!.create(
                SymbolOptions()
                    .withLatLng(LatLng(lat, lng))
                    .withIconImage(MARKER_DEFAULT)
            )

            // Attach data to marker
            val markerData = JsonObject()
            markerData.add("sensorId", JsonPrimitive(sensor.id))
            marker.data = markerData

            // Add click listener
            symbolManager!!.addClickListener {
                onMarkerSelected(it)
            }

            // Store location
            locations.add(location)
        }

        // Add map click listener
        map!!.addOnMapClickListener(MapboxMap.OnMapClickListener {
            Log.d(TAG, "Clicked on map")

            if (this@MapActivity.activeMarker == null) {
                return@OnMapClickListener true
            }

            // No more active marker
            this.deselectMarkers()

            // Hide the details pane
            this.hideBottomSheet()

            return@OnMapClickListener true
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
    private fun onMarkerSelected(marker: Symbol): Boolean {
        val sensorId: Int? = marker.data?.asJsonObject?.get("sensorId")?.asInt
        Log.d(TAG, "Selected marker ID: ${marker.id} (sensor ID: $sensorId)")
        if (sensorId == null) {
            return true
        }

        // Update active marker icon
        this.activeMarker?.let {
            it.iconImage = MARKER_DEFAULT
            symbolManager?.update(it)
        }
        marker.iconImage = MARKER_ACTIVE
        symbolManager?.update(marker)
        this.activeMarker = marker

        // Clear old data
        details_section_sponsor.visibility = View.GONE

        // Lookup sensor for that marker
        val sensorMeasurements = sensors[sensorId]
        if (sensorMeasurements == null) {
            Log.e(TAG, "Sensor with id $sensorId not found")
            Utils.showError(this, "Sensor not found")
            return true
        }
        val sensor = sensorMeasurements.sensor

        // Fetch sensor details asynchronously
        Log.i(TAG, "Fetching sensor " + sensor.id)
        this.progressCounter!!.increment()
        apiService!!.getSensorDetails(sensor.id).enqueue(onSensorDetailsFetched())

        // Look up sponsor in cache. If not found, fetch it asynchronously.
        if (sensor.sponsorId != null) {
            val sponsor = sponsors.get(sensor.sponsorId)
            if (sponsor == null) {
                // Not found in cache, fetch it from the API
                Log.i(TAG, "Fetching sponsor " + sensor.id)
                this.progressCounter!!.increment()
                apiService!!.getSponsor(sensor.id).enqueue(onSponsorFetched())
            } else {
                // Cache hit!
                Log.d(TAG, "Sponsor " + sensor.sponsorId + " cache hit")
                updateDetailsSponsor(sponsor)
            }
        }

        // Fetch sensor measurements from last three days
        // TODO: Use new API
        val since = Instant.now().minus(3, ChronoUnit.DAYS)
        val measurementCall = apiService!!.listMeasurementsSince(sensor.id, since)
        this.progressCounter!!.increment()
        measurementCall.enqueue(onMeasurementsFetched())

        // Get last temperature measurement
        val captionBuilder = StringBuilder()
        if (sensor.latestTemperature != null) {
            val pt = PrettyTime()
            captionBuilder.append(String.format("%.2f", sensor.latestTemperature))
            captionBuilder.append("°C (")
            // TODO: Once https://github.com/gfroerli/api/pull/80 is merged
            //val createdAtDate = Date(sensor.latestTemperature.createdAt.toInstant().toEpochMilli())
            //captionBuilder.append(pt.format(createdAtDate))
            captionBuilder.append("TODO time ago")
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

        // Show the details pane
        this.showBottomSheet()

        return true
    }

    /**
     * Deselect all markers and set the `activeMarker` attribute to `null`.
     */
    private fun deselectMarkers() {
        this.activeMarker?.let {
            it.iconImage = MARKER_DEFAULT
            this.symbolManager?.update(it)
            this.activeMarker = null
        }
    }

    private fun onSensorDetailsFetched(): Callback<SensorDetails> {
        return object : Callback<SensorDetails> {
            override fun onResponse(call: Call<SensorDetails>, response: Response<SensorDetails>?) {
                this@MapActivity.progressCounter!!.decrement()

                Log.i(TAG, "Processing sensor details response")
                response?.body()?.let {
                    updateDetailsDataSummary(it)
                }
            }

            override fun onFailure(call: Call<SensorDetails>, t: Throwable) {
                this@MapActivity.progressCounter!!.decrement()

                Log.e(TAG, "Fetching sensor details failed: $t")
                Utils.showError(
                        this@MapActivity,
                        getString(R.string.fetching_data_failed, getString(R.string.data_sensor_details))
                )
            }
        }
    }

    private fun onSponsorFetched(): Callback<Sponsor> {
        return object : Callback<Sponsor> {
            override fun onResponse(call: Call<Sponsor>, response: Response<Sponsor>?) {
                this@MapActivity.progressCounter!!.decrement()

                Log.i(TAG, "Processing sponsor response")
                response?.body()?.let {
                    // Update details
                    updateDetailsSponsor(it)

                    // Store in cache
                    this@MapActivity.sponsors.put(it.id, it)
                }
            }

            override fun onFailure(call: Call<Sponsor>, t: Throwable) {
                this@MapActivity.progressCounter!!.decrement()

                Log.e(TAG, "Fetching sponsor failed: $t")
                Utils.showError(
                        this@MapActivity,
                        getString(R.string.fetching_data_failed, getString(R.string.data_sponsor))
                )
            }
        }
    }

    private fun onMeasurementsFetched(): Callback<List<Measurement>> {
        return object : Callback<List<Measurement>> {
            override fun onResponse(call: Call<List<Measurement>>, response: Response<List<Measurement>>?) {
                this@MapActivity.progressCounter!!.decrement()

                Log.i(TAG, "Processing measurements response")
                response?.body()?.let {
                    drawChart3Days(it)
                }
            }

            override fun onFailure(call: Call<List<Measurement>>, t: Throwable) {
                this@MapActivity.progressCounter!!.decrement()

                Log.e(TAG, "Fetching measurements failed: $t")
                Utils.showError(
                    this@MapActivity,
                    getString(R.string.fetching_data_failed, getString(R.string.data_measurements))
                )
            }
        }
    }

    /**
     * Draw the temperature line chart for the measurements during the last 3 days.
     */
    private fun drawChart3Days(measurements: List<Measurement>) {
        // This is the start of the X axis
        val duration = Duration.of(3, ChronoUnit.DAYS)
        val startEpoch = Instant.now().minus(duration).toEpochMilli()

        // Create an entry for every measurement
        val entries: MutableList<Entry> = ArrayList()
        for (measurement in measurements) {
            val x = measurement.createdAt.toInstant().toEpochMilli() - startEpoch
            val y = measurement.temperature
            entries.add(Entry(x.toFloat(), y))
        }
        // See https://github.com/gfroerli/gfroerli-api/issues/40
        entries.sortBy { it.x }

        // Create a data set
        val dataSet = LineDataSet(entries, getString(R.string.temperature) + " (°C)")

        // X axis value range
        this@MapActivity.chart3days!!.xAxis.axisMinimum = 0f
        this@MapActivity.chart3days!!.xAxis.axisMaximum = duration.toMillis().toFloat()

        // Styling
        dataSet.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
        dataSet.lineWidth = 4f
        dataSet.circleRadius = 2f
        dataSet.color = resources.getColor(R.color.colorAccentAlpha)
        dataSet.setCircleColor(dataSet.color)
        dataSet.setDrawCircleHole(false)
        dataSet.setDrawHighlightIndicators(false)

        // Draw data
        val data = LineData(dataSet)
        data.setDrawValues(false)
        this@MapActivity.chart3days!!.data = data
        this@MapActivity.chart3days!!.invalidate()
    }

    /**
     * Update the summary of the sensor with aggregated data (min/max/avg temperature).
     */
    private fun updateDetailsDataSummary(sensorDetails: SensorDetails) {
        Log.d(TAG, "Update sensor ${sensorDetails.id} details: Summary")
        this.details_sensor_caption.text = "Min: %.1f°C | Max: %.1f°C | Avg: %.1f°C".format(
            sensorDetails.minimumTemperature, sensorDetails.maximumTemperature, sensorDetails.averageTemperature
        )
    }

    /**
     * Update the sponsor details of the sensor.
     */
    private fun updateDetailsSponsor(sponsor: Sponsor) {
        Log.d(TAG, "Update sensor details: Sponsor ${sponsor.id}")

        // Header
        details_sponsor_section_header.text = getString(R.string.section_header_sponsor, sponsor.name)

        // Description
        val sponsorDescriptionBuilder = StringBuilder()
        sponsorDescriptionBuilder.append(getString(R.string.sponsor_description, sponsor.name))
        sponsorDescriptionBuilder.append("\n")
        sponsorDescriptionBuilder.append(sponsor.description)
        details_sponsor_description.text = sponsorDescriptionBuilder.toString()

        // Logo
        if (sponsor.logoUrl != null) {
            val imageView: ImageView = findViewById(R.id.details_sponsor_logo)
            Glide.with(this).load(sponsor.logoUrl).into(imageView)
        }

        // Show section
        details_section_sponsor.visibility = View.VISIBLE
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        map_view!!.onSaveInstanceState(outState)
    }

    // Menu

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_about -> {
                Log.d(TAG, "Menu: About")
                val intent = Intent(this, AboutActivity::class.java)
                startActivity(intent)
            }
            R.id.action_refresh -> {
                Log.d(TAG, "Menu: Refresh")
                if (this.map != null) {
                    fetchInitialData()
                }
            }
            else -> Log.w(TAG, "Selected unknown menu entry: $item")
        }
        return super.onOptionsItemSelected(item)
    }

    // Key events

    override fun onBackPressed() {
        // If the bottom sheet is visible, close it on back button press.
        // Otherwise, fall back to default behavior.
        if (bottomSheetBehavior!!.state != BottomSheetBehavior.STATE_HIDDEN) {
            bottomSheetBehavior!!.state = BottomSheetBehavior.STATE_HIDDEN
        } else {
            super.onBackPressed()
        }
    }
}