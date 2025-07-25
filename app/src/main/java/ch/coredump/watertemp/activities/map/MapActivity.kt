package ch.coredump.watertemp.activities.map

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.util.SparseArray
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.BottomSheetValue
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.material.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle.Companion.Italic
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModelProvider
import ch.coredump.watertemp.BuildConfig
import ch.coredump.watertemp.Config
import ch.coredump.watertemp.R
import ch.coredump.watertemp.Utils
import ch.coredump.watertemp.rest.ApiClient
import ch.coredump.watertemp.rest.ApiService
import ch.coredump.watertemp.rest.SensorMeasurements
import ch.coredump.watertemp.rest.models.ApiMeasurement
import ch.coredump.watertemp.rest.models.ApiSensor
import ch.coredump.watertemp.rest.models.ApiSensorDetails
import ch.coredump.watertemp.rest.models.ApiSponsor
import ch.coredump.watertemp.theme.GfroerliColorsLight
import ch.coredump.watertemp.theme.GfroerliTypography
import ch.coredump.watertemp.ui.viewmodels.Measurement
import ch.coredump.watertemp.ui.viewmodels.Sensor
import ch.coredump.watertemp.ui.viewmodels.SensorBottomSheetViewModel
import ch.coredump.watertemp.ui.viewmodels.SensorStats
import ch.coredump.watertemp.ui.viewmodels.Sponsor
import ch.coredump.watertemp.utils.GfroerliThemeWrapper
import ch.coredump.watertemp.utils.LinkifyText
import ch.coredump.watertemp.utils.ProgressCounter
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.plugins.annotation.Symbol
import org.maplibre.android.plugins.annotation.SymbolManager
import org.maplibre.android.plugins.annotation.SymbolOptions
import org.ocpsoft.prettytime.PrettyTime
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

@ExperimentalMaterialApi
class MapActivity : ComponentActivity() {

    companion object {
        // Log tag
        private const val TAG = "MapActivity"
    }

    // The map instance
    private var map: MapLibreMap? = null
    private var symbolManager: SymbolManager? = null

    // Access the API service
    private var apiService: ApiService? = null

    // Mapping from sensor IDs to `SensorMeasurements` instances
    @SuppressLint("UseSparseArrays")
    internal val sensors = HashMap<Int, SensorMeasurements>()

    // Mapping from sponsor IDs to `Sponsor` instances
    internal val sponsors = SparseArray<ApiSponsor>()

    // The currently active marker
    private var activeMarker: Symbol? = null

    // View model including the currently active sensor and its data
    internal lateinit var viewModel: SensorBottomSheetViewModel

    // Activity indicator
    private lateinit var progressCounter: ProgressCounter

    // Animation values
    private var shortAnimationDuration: Int = 0
    private var colorAccentAlpha: Int? = null
    private lateinit var labelTemperature: String

    // Menu handler
    private lateinit var menu: MapActivityMenu

    // API response handler
    private lateinit var apiHandler: MapActivityApiHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get resource values
        this.shortAnimationDuration = resources.getInteger(android.R.integer.config_shortAnimTime)
        this.colorAccentAlpha = resources.getColor(R.color.colorAccentAlpha)
        this.labelTemperature = getString(R.string.temperature)

        // Initialize viewmodel
        viewModel = ViewModelProvider(this)[SensorBottomSheetViewModel::class.java]

        // Initialize the layout
        setContent {
            RootComposable(this.viewModel)
        }

        // Progress counter
        this.progressCounter = ProgressCounter()

        // Initialize menu handler
        menu = MapActivityMenu(this)

        // Initialize API handler
        apiHandler = MapActivityApiHandler(this, progressCounter, ::updateMarkers)

        // Get API client
        // TODO: Use singleton dependency injection using something like dagger 2
        val apiClient = ApiClient(BuildConfig.GFROERLI_API_KEY_PUBLIC)
        apiService = apiClient.apiService
    }

    override fun onDestroy() {
        symbolManager?.onDestroy()
        super.onDestroy()
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     */
    private fun onMapReady(mapLibreMap: MapLibreMap, mapView: MapView) {
        Log.d(TAG, "Map is ready")
        mapLibreMap.setStyle(Style.getPredefinedStyle("OUTDOORS")) { style ->
            Log.d(TAG, "Style loaded")
            initializeMapStyle(mapLibreMap, mapView, style)
        }
    }

    /**
     * Initialize the map style and symbol manager.
     * This should be called whenever the style is loaded or reloaded.
     */
    private fun initializeMapStyle(mapLibreMap: MapLibreMap, mapView: MapView, style: Style) {
        // Clean up existing symbol manager
        symbolManager?.onDestroy()

        // Create marker bitmaps
        for (markerType in MarkerType.entries) {
            addStyleMarker(style, markerType)
        }

        // Initialize symbol manager with proper configuration
        symbolManager = SymbolManager(mapView, mapLibreMap, style).apply {
            iconAllowOverlap = true
            iconIgnorePlacement = true
            textAllowOverlap = true
            textIgnorePlacement = true

            // Add click listener
            addClickListener { marker ->
                onMarkerSelected(marker)
                true
            }
        }

        // Save map as attribute
        this.map = mapLibreMap

        // Disable interactions that might confuse the user
        val settings = mapLibreMap.uiSettings
        settings.isRotateGesturesEnabled = false
        settings.isTiltGesturesEnabled = false
        settings.isCompassEnabled = false

        // Fetch initial data only after everything is set up
        if (sensors.isEmpty()) {
            this.fetchInitialData()
        } else {
            // If we already have sensor data, just update the markers
            updateMarkers()
        }
    }

    /**
     * Public method to refresh data, called by menu
     */
    fun refreshData() {
        if (this.map != null) {
            fetchInitialData()
        }
    }

    /**
     * Request initial data.
     */
    private fun fetchInitialData() {
        Log.d(TAG, "Fetching initial data from API")

        // Fetch sensors
        val sensorCall = apiService!!.listSensors()
        this.progressCounter.increment()
        sensorCall.enqueue(apiHandler.onSensorsFetched())

        // TODO: Do we need to re-fetch sensor-details of currently showing sensor?
    }



    /**
     * Remove all sensor markers and recreate them.
     */
    private fun updateMarkers() {
        // Clear old markers
        this.symbolManager!!.deleteAll()

        // Process sensors
        val locations = ArrayList<LatLng>()
        for (sensorMeasurement in sensors.values) {
            val sensor = sensorMeasurement.sensor
            Log.i(TAG, "Add sensor ${sensor.deviceName} (id=${sensor.id})")

            // Create location object
            val lat = sensor.latitude
            val lng = sensor.longitude
            if (lat == null || lng == null) {
                Log.w(TAG, "Skipping sensor without location: ${sensor.deviceName}")
                continue
            }
            val location = LatLng(lat, lng)

            // Create marker on map
            val iconImageName = MarkerType.forTemperature(sensor.latestTemperature).name
            val marker = this.symbolManager!!.create(
                SymbolOptions()
                    .withLatLng(LatLng(lat, lng))
                    .withIconImage(iconImageName)
                    .withTextField("${sensor.latestTemperature?.roundToInt() ?: "?"}")
                    .withTextSize(12f)
                    .withTextColor("#111111")
                    .withTextAnchor("center")
                    .withTextFont(arrayOf("Open Sans Bold"))
                    .withSymbolSortKey(sensor.id.toFloat()) // Control z-order
            )

            // Attach data to marker
            val markerData = JsonObject()
            markerData.add(MARKER_DATA_SENSOR_ID, JsonPrimitive(sensor.id))
            markerData.add(MARKER_DATA_ICON_IMAGE_NAME, JsonPrimitive(iconImageName))
            marker.data = markerData

            // Add click listener
            symbolManager!!.addClickListener {
                onMarkerSelected(it)
                true
            }

            // Store location
            locations.add(location)
        }

        // Add map click listener
        map!!.addOnMapClickListener(MapLibreMap.OnMapClickListener {
            Log.d(TAG, "Clicked on map")

            if (this@MapActivity.activeMarker == null) {
                return@OnMapClickListener true
            }

            // No more active marker
            this.deselectMarkers()

            // Hide the details pane
            viewModel.hideBottomSheet()

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
            map!!.moveCamera(CameraUpdateFactory.newLatLngBounds(boundingBoxBuilder.build(), 180))
        }
    }

    /**
     * Set the specified marker as the active marker.
     */
    private fun setMarkerAsActive(marker: Symbol) {
        marker.iconImage = MarkerType.ACTIVE.name
        symbolManager?.update(marker)
        this.activeMarker = marker
    }

    /**
     * Reset the marker type.
     */
    private fun resetMarkerType(marker: Symbol) {
        val originalIconImageName =
            marker.data?.asJsonObject?.get(MARKER_DATA_ICON_IMAGE_NAME)?.asString
        marker.iconImage = originalIconImageName ?: MarkerType.UNKNOWN.name
        this.symbolManager?.update(marker)
    }

    /**
     * Called when a marker is selected.
     */
    private fun onMarkerSelected(marker: Symbol) {
        val sensorId: Int? = marker.data?.asJsonObject?.get("sensorId")?.asInt
        Log.d(TAG, "Selected marker ID: ${marker.id} (sensor ID: $sensorId)")
        if (sensorId == null) {
            return
        }

        // Update active marker icon
        this.activeMarker?.let {
            this.resetMarkerType(it)
        }
        this.setMarkerAsActive(marker)

        // Lookup sensor for that marker
        val sensorMeasurements = sensors[sensorId]
        if (sensorMeasurements == null) {
            Log.e(TAG, "Sensor with id $sensorId not found")
            Utils.showError(this, "Sensor not found")
            return
        }
        val sensor = sensorMeasurements.sensor

        // Create viewmodel and update UI
        this.viewModel.setSensor(Sensor.fromApiSensor(sensor))

        // Fetch sensor details asynchronously
        Log.i(TAG, "Fetching sensor " + sensor.id)
        this.progressCounter.increment()
        apiService!!.getSensorDetails(sensor.id).enqueue(apiHandler.onSensorDetailsFetched())

        // Look up sponsor in cache. If not found, fetch it asynchronously.
        if (sensor.sponsorId != null) {
            val sponsor = sponsors.get(sensor.sponsorId)
            if (sponsor == null) {
                // Not found in cache, fetch it from the API
                Log.i(TAG, "Fetching sponsor ${sensor.id}")
                this.progressCounter.increment()
                apiService!!.getSponsor(sensor.id).enqueue(apiHandler.onSponsorFetched())
            } else {
                // Cache hit!
                Log.d(TAG, "Sponsor ${sensor.sponsorId} cache hit")
                this.viewModel.addSponsor(sponsor)
            }
        }

        // Fetch sensor measurements from last three days
        // TODO: Use new API
        val since = Instant.now().minus(3, ChronoUnit.DAYS)
        val measurementCall = apiService!!.listMeasurementsSince(sensor.id, since)
        this.progressCounter.increment()
        measurementCall.enqueue(apiHandler.onMeasurementsFetched())

        // Show the details pane (if not already visible)
        this.viewModel.showBottomSheet()

        return
    }

    /**
     * Deselect all markers and set the `activeMarker` attribute to `null`.
     */
    private fun deselectMarkers() {
        this.activeMarker?.let {
            this.resetMarkerType(it)
            this.activeMarker = null
        }
    }









    // Composable helpers

    class IgnoreBottomPadding(val wrapped: PaddingValues) : PaddingValues {
        override fun calculateBottomPadding(): Dp {
            // Override
            return 0.dp
        }

        override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp {
            return wrapped.calculateLeftPadding(layoutDirection)
        }

        override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp {
            return wrapped.calculateRightPadding(layoutDirection)
        }

        override fun calculateTopPadding(): Dp {
            return wrapped.calculateTopPadding()
        }
    }

    // Composables

    @Composable
    private fun RootComposable(viewModel: SensorBottomSheetViewModel) {
        // State: Show menu
        val showMenu = remember { mutableStateOf(false) }

        // State: Bottom sheet scaffold
        val scaffoldState = rememberBottomSheetScaffoldState(
            bottomSheetState = rememberBottomSheetState(BottomSheetValue.Collapsed)
        )

        // State: Bottom sheet visibility
        val showBottomSheet by viewModel.showBottomSheet.collectAsState()

        // State: Height of the bottom sheet peek pane
        val baseBottomSheetPeekHeight by remember { mutableStateOf(125.dp) }
        val bottomSheetPeekHeight = if (showBottomSheet) baseBottomSheetPeekHeight else 0.dp

        // State: Scroll state of the bottom sheet
        val sheetContentScrollState = rememberScrollState()

        // State: Coroutine scope
        val scope = rememberCoroutineScope()

        // Collapse bottom sheet if requested
        LaunchedEffect(showBottomSheet) {
            if (!showBottomSheet) {
                scope.launch { scaffoldState.bottomSheetState.collapse() }
            }
        }

        // Handle back event when bottom sheet is expanded
        BackHandler(enabled = scaffoldState.bottomSheetState.isExpanded) {
            // Scroll sheet content all the way to the top
            scope.launch { sheetContentScrollState.scrollTo(0) }
            // Collapse bottom sheet with an animation
            scope.launch { scaffoldState.bottomSheetState.collapse() }
        }

        // Handle back event when bottom sheet is collapsed
        BackHandler(enabled = scaffoldState.bottomSheetState.isCollapsed) {
            // Hide bottom sheet
            viewModel.hideBottomSheet()

            // Deselect markers
            scope.launch { deselectMarkers() }
        }

        // Wrap everything in our theme
        GfroerliThemeWrapper {
            // Use the scaffold with app bar and bottom sheet
            BottomSheetScaffold(
                scaffoldState = scaffoldState,

                // The app bar (AKA action bar)
                topBar = { TopBar(showMenu) },

                // Main content
                content = { innerPadding ->
                    // Note: Ignore bottom padding, because we want to avoid re-layouting the map
                    //       when showing / hiding the bottom sheet.
                    Box(Modifier.padding(IgnoreBottomPadding(innerPadding))) {
                        // MapLibre map
                        Map()

                        // Note: The progress indicator intentionally overlays the content
                        this@MapActivity.progressCounter.Composable()
                    }
                },

                // Bottom sheet
                sheetPeekHeight = bottomSheetPeekHeight,
                sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                sheetContent = {
                    // Wrap content in a box to allow overlaying the grab handle and the main content.
                    Box {
                        // Grab handle
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(50),
                                    )
                                    .size(width = 36.dp, height = 4.dp),
                            )
                        }

                        // Main content
                        Column(
                            Modifier
                                .verticalScroll(sheetContentScrollState)
                                .fillMaxWidth()
                        ) {
                            // Peek area (same height as sheetPeekHeight)
                            Box(
                                Modifier
                                    .height(bottomSheetPeekHeight)
                                    .padding(16.dp, 0.dp, 0.dp, 16.dp)
                                /*.swipeable(
                                    state = swipeableState,
                                    anchors = anchors,
                                    thresholds = { _, _ -> FractionalThreshold(0.3f) },
                                    orientation = Orientation.Vertical,
                                )*/
                            ) {
                                SensorPreview(viewModel)
                            }

                            // Divider
                            Divider()

                            // Expanded content
                            Box(Modifier.padding(16.dp)) {
                                SensorDetails(viewModel)
                            }
                        }
                    }
                },
            )
        }
    }

    @Composable
    fun TopBar(showMenu: MutableState<Boolean>) {
        TopAppBar(
            title = { Text(stringResource(id = R.string.activity_map)) },
            backgroundColor = MaterialTheme.colors.primary,
            actions = menu.topAppBarActions(showMenu),
        )
    }

    @Composable
    private fun Map(modifier: Modifier = Modifier) {
        AndroidView(
            modifier = modifier,
            factory = { context ->
                // Initialize maplibre
                MapLibre.getInstance(
                    context,
                    BuildConfig.MAPBOX_ACCESS_TOKEN,
                    WellKnownTileServer.Mapbox
                )
                val mapOptions = MapLibreMapOptions.createFromAttributes(context)
                    .logoEnabled(false)
                    .attributionMargins(intArrayOf(10, 10, 10, 10))
                    .camera(
                        CameraPosition.Builder()
                            .target(LatLng(47.209587, 8.823612))
                            .zoom(11.0)
                            .tilt(0.0)
                            .build()
                    )
                MapView(context, mapOptions).apply {
                    getMapAsync { map ->
                        onMapReady(map, this)
                    }
                }
            },
        )
    }

    /**
     * The sensor preview shown in the bottom sheet peek pane.
     */
    @Composable
    private fun SensorPreview(viewModel: SensorBottomSheetViewModel) {
        val sensor by viewModel.sensor.collectAsState()
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                viewModel.sensor.value?.name ?: "No sensor. If you can see this, that's a bug.",
                style = MaterialTheme.typography.h2,
                modifier = Modifier.padding(0.dp, 20.dp, 0.dp, 4.dp),
            )
            sensor?.let { sensor ->
                Text(
                    sensor.caption ?: "",
                    style = MaterialTheme.typography.body2,
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
                style = MaterialTheme.typography.body1,
                modifier = Modifier.padding(4.dp, 0.dp, 0.dp, 0.dp),
            )
        }
    }

    @Composable
    fun SensorDetails(viewModel: SensorBottomSheetViewModel) {
        val sensor by viewModel.sensor.collectAsState()
        val measurements by viewModel.measurements.collectAsState()

        Column() {
            sensor?.let { sensor ->
                // Section: History (last 3 days)
                Text(
                    stringResource(R.string.section_header_3days),
                    style = MaterialTheme.typography.h3,
                )
                Box(
                    modifier = Modifier.height(144.dp)
                ) {
                    if (measurements == null) {
                        LoadingDataText()
                    }
                    measurements?.let { measurements ->
                        if (measurements.isEmpty()) {
                            Text(
                                stringResource(R.string.chart_no_data),
                                style = MaterialTheme.typography.body1.plus(TextStyle(fontStyle = Italic))
                            )
                        } else {
                            TemperatureChart(
                                measurements,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight()
                            )
                        }
                    }
                }

                // Section: Summary
                Text(
                    stringResource(R.string.section_header_summary),
                    style = MaterialTheme.typography.h3,
                    modifier = Modifier.padding(0.dp, 16.dp, 0.dp, 4.dp),
                )
                if (sensor.statsAllTime == null) {
                    LoadingDataText()
                } else {
                    sensor.statsAllTime.let {
                        Text(
                            "Min: %.1f°C | Max: %.1f°C | Avg: %.1f°C".format(
                                it.minTemp,
                                it.maxTemp,
                                it.avgTemp
                            ),
                            style = MaterialTheme.typography.body1,
                        )
                    }
                }
                sensor.statsAllTime?.let {

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
                        style = MaterialTheme.typography.body1,
                    )
                    it.logoUrl?.let { url ->
                        GlideImage(
                            imageModel = { url },
                            imageOptions = ImageOptions(
                                contentScale = ContentScale.Fit,
                                alignment = Alignment.Center,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp, 24.dp),
                            previewPlaceholder = painterResource(id = R.drawable.app_icon_foreground),
                        )
                    }
                    it.description?.let { description ->
                        LinkifyText(
                            description,
                            style = MaterialTheme.typography.body1,
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

    @Composable
    fun LoadingDataText() {
        Text(
            stringResource(R.string.loading_data),
            style = MaterialTheme.typography.body1.plus(TextStyle(fontStyle = Italic)),
        )
    }

    @Preview
    @Composable
    fun PreviewSensor() {
        val viewModel = SensorBottomSheetViewModel.fromSensor(Sensor(
            "Testsensor",
            "The bestest sensor of all!",
            Measurement(ZonedDateTime.now(), 13.373737f),
            SensorStats(3.7, 31.2, 14.56),
            Sponsor(
                "Reynholm Industries",
                "Our primary focus is on trending and disruptive technologies and their potential impacts on existing markets!",
                "https://www.reynholm.industries/images/logo/logo.png"
            )
        ))
        MaterialTheme(
            colors = GfroerliColorsLight,
            typography = GfroerliTypography,
        ) {
            Column {
                SensorPreview(viewModel)
                Spacer(modifier = Modifier.height(24.dp))
                SensorDetails(viewModel)
            }
        }
    }
}
