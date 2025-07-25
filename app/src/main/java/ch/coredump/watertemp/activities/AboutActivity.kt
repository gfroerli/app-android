package ch.coredump.watertemp.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.DrawerState
import androidx.compose.material.DrawerValue
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.coredump.watertemp.BuildConfig
import ch.coredump.watertemp.R
import ch.coredump.watertemp.utils.BottomSpacer
import ch.coredump.watertemp.utils.GfroerliThemeWrapper
import ch.coredump.watertemp.utils.LinkifyText
import ch.coredump.watertemp.utils.bottomSpacerHeight
import com.composables.core.ScrollArea
import com.composables.core.Thumb
import com.composables.core.VerticalScrollbar
import com.composables.core.rememberScrollAreaState

private const val TAG = "AboutActivity"

class AboutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
        val scrollAreaState = rememberScrollAreaState(scrollState)

        // Wrap everything in our theme
        GfroerliThemeWrapper {
            Scaffold(
                scaffoldState = scaffoldState,

                // The app bar (AKA action bar)
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(id = R.string.activity_about)) },
                        backgroundColor = MaterialTheme.colors.primary,
                        navigationIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                modifier = Modifier
                                    .padding(12.dp)
                                    .clickable(onClick = { this.finish() }),
                                tint = Color.White
                            )
                        },
                    )
                },

                // Main content
                content = { innerPadding ->
                    ScrollArea(
                        state = scrollAreaState,
                        modifier = Modifier.padding(innerPadding),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp).verticalScroll(scrollState),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(stringResource(R.string.version_heading), style = MaterialTheme.typography.h2)
                            Text(stringResource(R.string.version_text1, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE), style = MaterialTheme.typography.body1)

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(stringResource(R.string.about_project_heading), style = MaterialTheme.typography.h2)
                            Text(stringResource(R.string.about_project_text1), style = MaterialTheme.typography.body1)
                            Text(stringResource(R.string.about_project_text2), style = MaterialTheme.typography.body1)
                            Text(stringResource(R.string.about_project_text3), style = MaterialTheme.typography.body1)
                            LinkifyText(stringResource(R.string.about_project_text4), style = MaterialTheme.typography.body1)

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(stringResource(R.string.about_us_heading), style = MaterialTheme.typography.h2)
                            Text(stringResource(R.string.about_us_text1), style = MaterialTheme.typography.body1)
                            LinkifyText(stringResource(R.string.about_us_text2), style = MaterialTheme.typography.body1)

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(stringResource(R.string.placement_heading), style = MaterialTheme.typography.h2)
                            LinkifyText(stringResource(R.string.placement_text1), style = MaterialTheme.typography.body1)

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(stringResource(R.string.open_source_heading), style = MaterialTheme.typography.h2)
                            LinkifyText(stringResource(R.string.open_source_text1), style = MaterialTheme.typography.body1)

                            BottomSpacer()
                        }

                        VerticalScrollbar(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .fillMaxHeight()
                                .width(4    .dp)
                                .padding(top = 4.dp, bottom = bottomSpacerHeight() + 4.dp)
                        ) {
                            Thumb(
                                modifier = Modifier.background(
                                    Color.Black.copy(0.3f), RoundedCornerShape(100)
                                ),
                            )
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