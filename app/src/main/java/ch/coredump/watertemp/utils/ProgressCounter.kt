package ch.coredump.watertemp.utils

import android.util.Log
import android.view.View

private const val TAG = "ProgressCounter"

/**
 * Track multiple progress events.
 *
 * Show the progress view when the first event is started,
 * hide it when the last event has stopped.
 */
class ProgressCounter(private val progressView: View) {
    var count = 0

    /**
     * Start a progress activitiy.
     */
    @Synchronized
    fun increment() {
        count += 1
        if (count == 1) {
            this.progressView.visibility = View.VISIBLE
        }
    }

    /**
     * Stop a progress activitiy.
     */
    @Synchronized
    fun decrement() {
        if (count == 0) {
            Log.w(TAG, "Warning: Count already 0")
            return
        }
        count -= 1
        if (count == 0) {
            this.progressView.visibility = View.INVISIBLE
        }
    }
}