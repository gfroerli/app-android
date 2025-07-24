package ch.coredump.watertemp.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import ch.coredump.watertemp.BuildConfig
import ch.coredump.watertemp.R
import ch.coredump.watertemp.theme.GfroerliColorsLight
import ch.coredump.watertemp.theme.GfroerliTypography
import ch.coredump.watertemp.utils.LinkifyText

private const val TAG = "AboutActivity"

class AboutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge mode (can be removed once upgrading to API 35)
        enableEdgeToEdge()
        // Set status bar color to match TopAppBar
        window.statusBarColor = android.graphics.Color.parseColor("#1565c0")
        // Set light status bar content for better contrast with blue background
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false

        // Initialize the layout
        setContent {
            RootComposable()
        }
    }

    // Composables

    @Composable
    private fun RootComposable() {
        // State: Scaffold
        val scaffoldState = rememberScaffoldState(DrawerState(DrawerValue.Closed))

        // State: Scroll state
        val scrollState = rememberScrollState()

        // Wrap everything in our theme
        MaterialTheme(
            colors = GfroerliColorsLight,
            typography = GfroerliTypography,
        ) {
            Scaffold(
                scaffoldState = scaffoldState,

                // The app bar (AKA action bar)
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(id = R.string.activity_about)) },
                        backgroundColor = MaterialTheme.colors.primary,
                        navigationIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                modifier = Modifier
                                    .padding(12.dp)
                                    .clickable(onClick = { this.finish() }),
                                tint = Color.White
                            )
                        },
                        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
                    )
                },

                // Main content
                content = { innerPadding ->
                    Box(
                        modifier = Modifier.padding(innerPadding).verticalScroll(scrollState)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(stringResource(R.string.version_heading), style = MaterialTheme.typography.h2)
                            Text(stringResource(R.string.version_text1, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE), style = MaterialTheme.typography.body1)

                            Text(stringResource(R.string.about_project_heading), style = MaterialTheme.typography.h2)
                            Text(stringResource(R.string.about_project_text1), style = MaterialTheme.typography.body1)
                            Text(stringResource(R.string.about_project_text2), style = MaterialTheme.typography.body1)
                            LinkifyText(stringResource(R.string.about_project_text3), style = MaterialTheme.typography.body1)

                            Text(stringResource(R.string.about_us_heading), style = MaterialTheme.typography.h2)
                            Text(stringResource(R.string.about_us_text1), style = MaterialTheme.typography.body1)
                            LinkifyText(stringResource(R.string.about_us_text2), style = MaterialTheme.typography.body1)

                            Text(stringResource(R.string.open_source_heading), style = MaterialTheme.typography.h2)
                            LinkifyText(stringResource(R.string.open_source_text1), style = MaterialTheme.typography.body1)
                        }
                    }
                }
            )
        }
    }

    @Preview
    @Composable
    fun Preview() {
        RootComposable()
    }
}