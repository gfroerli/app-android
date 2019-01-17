package ch.coredump.watertemp.utils

import android.util.Log
import android.view.View

/**
 * Track multiple progress events.
 *
 * Show the progress view when the first event is started,
 * hide it when the last event has stopped.
 */
class ProgressCounter(val progressView: View) {
    val TAG = "ProgressCounter"

    var count = 0

    /**
     * Start a progress activitiy.
     */
    @Synchronized
    fun start() {
        count += 1
        if (count == 1) {
            this.progressView.visibility = View.VISIBLE
        }
    }

    /**
     * Stop a progress activitiy.
     */
    @Synchronized
    fun stop() {
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