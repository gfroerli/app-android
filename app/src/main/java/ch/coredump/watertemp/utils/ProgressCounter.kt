package ch.coredump.watertemp.utils

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NoLiveLiterals
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier

private const val TAG = "ProgressCounter"

/**
 * Track multiple progress events.
 *
 * Show the progress indicator when the first event is started,
 * hide it when the last event has stopped.
 */
@NoLiveLiterals
class ProgressCounter {
    private var count = mutableIntStateOf(0)

    /**
     * Start a progress activitiy.
     */
    @Synchronized
    fun increment() {
        count.intValue += 1
    }

    /**
     * Stop a progress activitiy.
     */
    @Synchronized
    fun decrement() {
        if (count.intValue == 0) {
            Log.w(TAG, "Warning: Count already 0")
            return
        }
        count.intValue -= 1
    }

    @Composable
    fun Composable() {
        if (this.count.intValue > 0) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}