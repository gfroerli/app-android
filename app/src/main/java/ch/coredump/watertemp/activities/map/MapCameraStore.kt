package ch.coredump.watertemp.activities.map

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng

/**
 * Remembers the map camera position, so that the map can be restored where the
 * user left off when the app is opened again.
 */
class MapCameraStore(context: Context) {

    companion object {
        private const val TAG = "MapCameraStore"

        private const val PREFERENCES_NAME = "map_camera"
        private const val KEY_LATITUDE = "latitude"
        private const val KEY_LONGITUDE = "longitude"
        private const val KEY_ZOOM = "zoom"
    }

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    /**
     * Store the current camera position.
     */
    fun save(cameraPosition: CameraPosition) {
        val target = cameraPosition.target ?: return
        Log.d(TAG, "Storing camera position")
        preferences.edit {
            putDouble(KEY_LATITUDE, target.latitude)
            putDouble(KEY_LONGITUDE, target.longitude)
            putDouble(KEY_ZOOM, cameraPosition.zoom)
        }
    }

    /**
     * Return the stored camera position, or null if the map was never shown before.
     */
    fun load(): CameraPosition? {
        if (!preferences.contains(KEY_LATITUDE)) {
            Log.d(TAG, "No stored camera position")
            return null
        }
        return CameraPosition.Builder()
            .target(
                LatLng(
                    preferences.getDouble(KEY_LATITUDE),
                    preferences.getDouble(KEY_LONGITUDE),
                )
            )
            .zoom(preferences.getDouble(KEY_ZOOM))
            .build()
    }
}

// Shared preferences have no native support for doubles, so store them as raw bits

private fun SharedPreferences.Editor.putDouble(key: String, value: Double) {
    putLong(key, value.toRawBits())
}

private fun SharedPreferences.getDouble(key: String): Double {
    return Double.fromBits(getLong(key, 0))
}
