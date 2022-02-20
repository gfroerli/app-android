package ch.coredump.watertemp.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.SparseArray
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle.Companion.Italic
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import ch.coredump.watertemp.BuildConfig
import ch.coredump.watertemp.Config
import ch.coredump.watertemp.R
import ch.coredump.watertemp.Utils
import ch.coredump.watertemp.databinding.ActivityMapBinding
import ch.coredump.watertemp.rest.ApiClient
import ch.coredump.watertemp.rest.ApiService
import ch.coredump.watertemp.rest.SensorMeasurements
import ch.coredump.watertemp.rest.models.ApiMeasurement
import ch.coredump.watertemp.rest.models.ApiSensor
import ch.coredump.watertemp.rest.models.ApiSensorDetails
import ch.coredump.watertemp.rest.models.ApiSponsor
import ch.coredump.watertemp.theme.GfroerliTypography
import ch.coredump.watertemp.ui.viewmodels.*
import ch.coredump.watertemp.utils.ProgressCounter
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.WellKnownTileServer
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.Symbol
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.skydoves.landscapist.glide.GlideImage
import org.ocpsoft.prettytime.PrettyTime
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit


// Marker image names
private const val MARKER_DEFAULT = "marker_default"
private const val MARKER_ACTIVE = "marker_active"

// Log tag
private const val TAG = "MapActivity"

class MapActivity : AppCompatActivity(), OnMapReadyCallback {
    // View bindings
    private lateinit var binding: ActivityMapBinding

    // The map instance
    private var map: MapboxMap? = null
    private var symbolManager: SymbolManager? = null

    // Access the water-sensor service
    private var apiService: ApiService? = null

    // Mapping from sensor IDs to `SensorMeasurements` instances
    @SuppressLint("UseSparseArrays")
    private val sensors = HashMap<Int, SensorMeasurements>()

    // Mapping from sponsor IDs to `Sponsor` instances
    private val sponsors = SparseArray<ApiSponsor>()

    // The currently active marker
    private var activeMarker: Symbol? = null

    // The currently active sensor (and its data)
    private val sensor: SensorViewModel = SensorViewModel()

    // Class to control how the bottom sheet behaves
    private var bottomSheetBehavior: BottomSheetBehavior<*>? = null

    // Activity indicator
    private var progressCounter: ProgressCounter? = null

    // Animation values
    private var shortAnimationDuration: Int = 0
    private var colorAccentAlpha: Int? = null
    private lateinit var labelTemperature: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize mapbox
        Mapbox.getInstance(this, BuildConfig.MAPBOX_ACCESS_TOKEN, WellKnownTileServer.Mapbox)

        // Initialize the layout
        this.binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(this.binding.root)
        this.binding.bottomSheetPeek.sensorCompose.setContent {
            MaterialTheme(typography = GfroerliTypography) {
                SensorPreview(this.sensor)
            }
        }
        this.binding.bottomSheetDetails.sensorDetailsCompose.setContent {
            MaterialTheme(typography = GfroerliTypography) {
                SensorDetails(this.sensor)
            }
        }

        // Initialize the action bar
        setSupportActionBar(this.binding.mainActionBar)
        supportActionBar!!.title = getString(R.string.activity_map)

        // Get resource values
        shortAnimationDuration = resources.getInteger(android.R.integer.config_shortAnimTime)
        colorAccentAlpha = resources.getColor(R.color.colorAccentAlpha)
        labelTemperature = getString(R.string.temperature)

        // Progress counter
        this.progressCounter = ProgressCounter(binding.loadingbar)

        // Create map view
        this.binding.mapView.onCreate(savedInstanceState)

        // Initialize map
        this.binding.mapView.getMapAsync(this)

        // Get API client
        // TODO: Use singleton dependency injection using something like dagger 2
        val apiClient = ApiClient(BuildConfig.GFROERLI_API_KEY_PUBLIC)
        apiService = apiClient.apiService

        // Initialize bottom sheet behavior
        this.bottomSheetBehavior = BottomSheetBehavior.from(this.binding.detailsBottomSheet)

        // Initially hidden
        this.bottomSheetBehavior!!.state = BottomSheetBehavior.STATE_HIDDEN

        // We want two sizes: Peek height and full height
        this.bottomSheetBehavior!!.isFitToContents = false

        // Add bottom sheet listener
        this.bottomSheetBehavior!!.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                // Bottom sheet state changed

                // Deselect markers when hidden
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    // Deselect all markers
                    this@MapActivity.deselectMarkers()
                }

                // Show/hide grab handle
                val grabHandle = this@MapActivity.binding.bottomSheetPeek.grabHandle
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    grabHandle.animate()
                        .alpha(0f)
                        .setDuration(shortAnimationDuration.toLong())
                        .setListener(null)
                } else {
                    grabHandle.animate()
                        .alpha(1f)
                        .setDuration(shortAnimationDuration.toLong())
                        .setListener(null)
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // Called repeatedly while bottom sheet slides up
            }
        })
    }

    override fun onStart() {
        super.onStart()
        this.binding.mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        this.binding.mapView.onStop()
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     */
    override fun onMapReady(mapboxMap: MapboxMap) {
        Log.d(TAG, "Map is ready")

        mapboxMap.setStyle(Style.getPredefinedStyle("OUTDOORS")) { style ->
            Log.d(TAG, "Style loaded")

            // Load marker icon
            style.addImage(MARKER_DEFAULT, ContextCompat.getDrawable(this, R.drawable.blue_marker)!!)
            style.addImage(MARKER_ACTIVE, ContextCompat.getDrawable(this, com.mapbox.mapboxsdk.R.drawable.mapbox_marker_icon_default)!!)

            // Initialize symbol manager
            this.symbolManager = SymbolManager(this.binding.mapView, mapboxMap, style)

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

    private fun onSensorsFetched(): Callback<List<ApiSensor>> {
        return object : Callback<List<ApiSensor>> {
            override fun onResponse(call: Call<List<ApiSensor>>, response: Response<List<ApiSensor>>?) {
                this@MapActivity.progressCounter!!.decrement()

                // Handle null response
                if (response == null) {
                    Log.e(TAG, "Received null response from sensors endpoint")
                    return
                }

                // Handle unsuccessful response
                if (!response.isSuccessful) {
                    val error = ApiClient.parseError(response)
                    Log.e(TAG, "Could not fetch sensors (HTTP " + error.statusCode + "): " + error.message)
                    Utils.showError(
                        this@MapActivity,
                        getString(R.string.fetching_sensor_data_failed, error.statusCode, Config.SUPPORT_EMAIL)
                    )
                    return
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

            override fun onFailure(call: Call<List<ApiSensor>>, t: Throwable) {
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
            map!!.moveCamera(CameraUpdateFactory.newLatLngBounds(boundingBoxBuilder.build(), 350))
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

        // Lookup sensor for that marker
        val sensorMeasurements = sensors[sensorId]
        if (sensorMeasurements == null) {
            Log.e(TAG, "Sensor with id $sensorId not found")
            Utils.showError(this, "Sensor not found")
            return true
        }
        val sensor = sensorMeasurements.sensor

        // Create viewmodel and update UI
        this.sensor.setSensor(Sensor.fromApiSensor(sensor))

        // Fetch sensor details asynchronously
        Log.i(TAG, "Fetching sensor " + sensor.id)
        this.progressCounter!!.increment()
        apiService!!.getSensorDetails(sensor.id).enqueue(onSensorDetailsFetched())

        // Look up sponsor in cache. If not found, fetch it asynchronously.
        if (sensor.sponsorId != null) {
            val sponsor = sponsors.get(sensor.sponsorId)
            if (sponsor == null) {
                // Not found in cache, fetch it from the API
                Log.i(TAG, "Fetching sponsor ${sensor.id}")
                this.progressCounter!!.increment()
                apiService!!.getSponsor(sensor.id).enqueue(onSponsorFetched())
            } else {
                // Cache hit!
                Log.d(TAG, "Sponsor ${sensor.sponsorId} cache hit")
                this.sensor.addSponsor(sponsor)
            }
        }

        // Fetch sensor measurements from last three days
        // TODO: Use new API
        val since = Instant.now().minus(3, ChronoUnit.DAYS)
        val measurementCall = apiService!!.listMeasurementsSince(sensor.id, since)
        this.progressCounter!!.increment()
        measurementCall.enqueue(onMeasurementsFetched())

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

    private fun onSensorDetailsFetched(): Callback<ApiSensorDetails> {
        return object : Callback<ApiSensorDetails> {
            override fun onResponse(call: Call<ApiSensorDetails>, response: Response<ApiSensorDetails>?) {
                this@MapActivity.progressCounter!!.decrement()

                Log.i(TAG, "Processing sensor details response")
                response?.body()?.let { details ->
                    this@MapActivity.sensor.addDetails(details)
                }
            }

            override fun onFailure(call: Call<ApiSensorDetails>, t: Throwable) {
                this@MapActivity.progressCounter!!.decrement()

                Log.e(TAG, "Fetching sensor details failed: $t")
                Utils.showError(
                        this@MapActivity,
                        getString(R.string.fetching_data_failed, getString(R.string.data_sensor_details))
                )
            }
        }
    }

    private fun onSponsorFetched(): Callback<ApiSponsor> {
        return object : Callback<ApiSponsor> {
            override fun onResponse(call: Call<ApiSponsor>, response: Response<ApiSponsor>?) {
                this@MapActivity.progressCounter!!.decrement()

                Log.i(TAG, "Processing sponsor response")
                response?.body()?.let { sponsor ->
                    // Update sensor
                    this@MapActivity.sensor.addSponsor(sponsor)

                    // Store in cache
                    this@MapActivity.sponsors.put(sponsor.id, sponsor)
                }
            }

            override fun onFailure(call: Call<ApiSponsor>, t: Throwable) {
                this@MapActivity.progressCounter!!.decrement()

                Log.e(TAG, "Fetching sponsor failed: $t")
                Utils.showError(
                        this@MapActivity,
                        getString(R.string.fetching_data_failed, getString(R.string.data_sponsor))
                )
            }
        }
    }

    private fun onMeasurementsFetched(): Callback<List<ApiMeasurement>> {
        return object : Callback<List<ApiMeasurement>> {
            override fun onResponse(call: Call<List<ApiMeasurement>>, response: Response<List<ApiMeasurement>>?) {
                this@MapActivity.progressCounter!!.decrement()

                Log.i(TAG, "Processing measurements response")
                response?.body()?.let {
                    this@MapActivity.sensor
                        .setMeasurements(
                            it.map(Measurement::fromApiMeasurement)
                        )
                }
            }

            override fun onFailure(call: Call<List<ApiMeasurement>>, t: Throwable) {
                this@MapActivity.progressCounter!!.decrement()

                Log.e(TAG, "Fetching measurements failed: $t")
                Utils.showError(
                    this@MapActivity,
                    getString(R.string.fetching_data_failed, getString(R.string.data_measurements))
                )
            }
        }
    }

    // Lifecycle methods

    public override fun onResume() {
        super.onResume()
        this.binding.mapView.onResume()
    }

    public override fun onPause() {
        super.onPause()
        this.binding.mapView.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        this.binding.mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        this.binding.mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        this.binding.mapView.onSaveInstanceState(outState)
    }

    // Menu

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
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

    // Composables

    /**
     * The sensor preview shown in the bottom sheet peek pane.
     */
    @Composable
    private fun SensorPreview(viewModel: SensorViewModel) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                viewModel.sensor.value?.name ?: "No sensor",
                style = MaterialTheme.typography.h2,
                modifier = Modifier.padding(0.dp, 20.dp, 0.dp, 4.dp),
            )
            viewModel.sensor.value?.let { sensor ->
                Text(
                    sensor.caption ?: "",
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier
                        .padding(0.dp, 0.dp, 0.dp, 8.dp)
                        .horizontalScroll(ScrollState(0)),
                )
                sensor.latestMeasurement?.let { measurement ->
                    SensorMeasurement(measurement)
                }
            }
        }
    }

    @Composable
    private fun SensorMeasurement(measurement: Measurement) {
        val pt = PrettyTime()
        val summary = "%.2f °C (%s)".format(
            measurement.temperature,
            pt.format(measurement.timestamp)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.temperature),
                modifier = Modifier
                    .width(16.dp)
                    .height(16.dp)
                    .offset((-2).dp, 0.dp),
                contentDescription = "Temperature icon",
            )
            Text(
                summary,
                style = MaterialTheme.typography.body2,
                modifier = Modifier.padding(4.dp, 0.dp, 0.dp, 0.dp),
            )
        }
    }

    @Composable
    fun SensorDetails(viewModel: SensorViewModel) {
        Column(modifier = Modifier.fillMaxWidth()) {
            viewModel.sensor.value?.let { sensor ->
                // Section: History (last 3 days)
                Text(
                    stringResource(R.string.section_header_3days),
                    style = MaterialTheme.typography.h3,
                )
                if (viewModel.measurements.value == null) {
                    Text(
                        stringResource(R.string.loading_data),
                        style = MaterialTheme.typography.body2.plus(TextStyle(fontStyle = Italic)),
                    )
                }
                viewModel.measurements.value?.let { measurements ->
                    if (measurements.isEmpty()) {
                        Text(
                            stringResource(R.string.chart_no_data),
                            style = MaterialTheme.typography.body2.plus(TextStyle(fontStyle = Italic))
                        )
                    } else {
                        TemperatureChart(
                            measurements,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(144.dp)
                        )
                    }
                }

                // Section: Summary
                sensor.statsAllTime?.let {
                    Text(
                        stringResource(R.string.section_header_summary),
                        style = MaterialTheme.typography.h3,
                        modifier = Modifier.padding(0.dp, 16.dp, 0.dp, 4.dp),
                    )
                    Text(
                        "Min: %.1f°C | Max: %.1f°C | Avg: %.1f°C".format(
                            it.minTemp,
                            it.maxTemp,
                            it.avgTemp
                        ),
                        style = MaterialTheme.typography.body2,
                    )
                }

                // Section: Sponsor
                sensor.sponsor?.let {
                    Text(
                        stringResource(R.string.section_header_sponsor, it.name),
                        style = MaterialTheme.typography.h3,
                        modifier = Modifier.padding(0.dp, 16.dp, 0.dp, 4.dp),
                    )
                    Text(
                        stringResource(R.string.sponsor_description, it.name),
                        style = MaterialTheme.typography.body2,
                    )
                    it.logoUrl?.let { url ->
                        GlideImage(
                            imageModel = url,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp, 24.dp),
                            previewPlaceholder = R.drawable.app_icon_foreground,
                        )
                    }
                    it.description?.let { description ->
                        Text(
                            description,
                            style = MaterialTheme.typography.body2,
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun TemperatureChart(measurements: List<Measurement>, modifier: Modifier) {
        AndroidView(
            modifier = modifier,
            factory = { context -> LineChart(context).apply {
                // Basic styling
                setNoDataText(context.getString(R.string.chart_no_data))
                setDrawGridBackground(false)
                setDrawBorders(false)
                description.isEnabled = false
                xAxis.isEnabled = false
                axisRight.isEnabled = false
            }},
            update = { chart ->
                // This is the start of the X axis
                val duration = Duration.of(3, ChronoUnit.DAYS)
                val startEpoch = Instant.now().minus(duration).toEpochMilli()

                // Create an entry for every measurement
                val entries: MutableList<Entry> = ArrayList()
                for (measurement in measurements) {
                    val x = measurement.timestamp.toInstant().toEpochMilli() - startEpoch
                    val y = measurement.temperature
                    entries.add(Entry(x.toFloat(), y))
                }
                // See https://github.com/gfroerli/gfroerli-api/issues/40
                entries.sortBy { it.x }

                // Create a data set
                val dataSet = LineDataSet(entries, "$labelTemperature (°C)")

                // X axis value range
                chart.xAxis.axisMinimum = 0f
                chart.xAxis.axisMaximum = duration.toMillis().toFloat()

                // Styling
                dataSet.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
                dataSet.lineWidth = 4f
                dataSet.circleRadius = 2f
                this.colorAccentAlpha?.let { dataSet.color = it }
                dataSet.setCircleColor(dataSet.color)
                dataSet.setDrawCircleHole(false)
                dataSet.setDrawHighlightIndicators(false)

                // Draw data
                val data = LineData(dataSet)
                data.setDrawValues(false)
                chart.data = data
                chart.invalidate()
            }
        )
    }

    @Preview
    @Composable
    fun PreviewSensor() {
        val viewModel = SensorViewModel.fromSensor(Sensor(
            "Testsensor",
            "The bestest sensor of all!",
            Measurement(ZonedDateTime.now(), 13.37373737f),
            SensorStats(3.7, 31.2, 14.56),
            Sponsor(
                "Reynholm Industries",
                "Our primary focus is on trending and disruptive technologies and their potential impacts on existing markets!",
                "https://www.reynholm.industries/images/logo/logo.png"
            )
        ))
        MaterialTheme(typography = GfroerliTypography) {
            Column {
                SensorPreview(viewModel)
                Spacer(modifier = Modifier.height(24.dp))
                SensorDetails(viewModel)
            }
        }
    }

}