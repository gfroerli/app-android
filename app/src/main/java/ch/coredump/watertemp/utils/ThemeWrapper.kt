package ch.coredump.watertemp.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ch.coredump.watertemp.theme.GfroerliColorsLight
import ch.coredump.watertemp.theme.GfroerliTypography

@Composable
fun GfroerliThemeWrapper(content: @Composable () -> Unit) {
    // Wrap everything in our theme
    MaterialTheme(
        colors = GfroerliColorsLight,
        typography = GfroerliTypography,
    ) {
        Column {
            // Spacer to extend top bar across status bar in edge-to-edge mode
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(MaterialTheme.colors.primary)
            )

            // Main content
            content()
        }
    }
}