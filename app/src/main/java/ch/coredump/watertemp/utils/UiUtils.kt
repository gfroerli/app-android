package ch.coredump.watertemp.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

@Composable
fun bottomSpacerHeight(): Dp {
    return WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
}

/**
 * Bottom spacer to compensate for rounded device corners (avoid clipping).
 */
@Composable
fun BottomSpacer(backgroundColor: Color = MaterialTheme.colors.background) {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(bottomSpacerHeight())
            .background(color = backgroundColor)
    )
}