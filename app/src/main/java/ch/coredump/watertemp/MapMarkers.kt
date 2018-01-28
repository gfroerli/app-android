package ch.coredump.watertemp

import android.content.Context
import com.mapbox.mapboxsdk.annotations.Icon
import com.mapbox.mapboxsdk.annotations.IconFactory

class MapMarkers constructor(context: Context) {
    val defaultIcon: Icon
    val activeIcon: Icon

    init {
        val iconFactory = IconFactory.getInstance(context)
        defaultIcon = iconFactory.fromResource(R.drawable.blue_marker)
        activeIcon = iconFactory.fromResource(R.drawable.mapbox_marker_icon_default)
    }
}