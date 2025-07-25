package ch.coredump.watertemp.activities.map

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.res.stringResource
import ch.coredump.watertemp.R
import ch.coredump.watertemp.activities.AboutActivity

/**
 * Handles menu functionality for the MapActivity
 */
@ExperimentalMaterialApi
class MapActivityMenu(private val activity: MapActivity) {

    companion object {
        private const val TAG = "MapActivityMenu"
    }

    enum class MenuItem {
        REFRESH, ABOUT
    }

    /**
     * Called when a menu entry from the app bar dropdown menu has been selected.
     */
    fun onMenuItemSelected(item: MenuItem, showMenu: MutableState<Boolean>?) {
        Log.d(TAG, "Menu: $item")

        // Hide menu
        showMenu?.value = false

        // Dispatch item
        when (item) {
            MenuItem.REFRESH -> {
                activity.refreshData()
            }
            MenuItem.ABOUT -> {
                val intent = Intent(activity, AboutActivity::class.java)
                activity.startActivity(intent)
            }
        }
    }

    /**
     * Returns the TopAppBar actions content
     */
    @Composable
    fun topAppBarActions(showMenu: MutableState<Boolean>): @Composable RowScope.() -> Unit = {
        OverflowMenu({
            DropdownMenuItem(
                onClick = { onMenuItemSelected(MenuItem.REFRESH, showMenu) }
            ) {
                Text(stringResource(id = R.string.action_refresh_all))
            }
            DropdownMenuItem(
                onClick = { onMenuItemSelected(MenuItem.ABOUT, showMenu) }
            ) {
                Text(stringResource(id = R.string.action_about_this_app))
            }
        }, showMenu)
    }

    /**
     * Simple reusable overflow menu.
     *
     * Source: https://stackoverflow.com/a/68354402/284318
     */
    @Composable
    fun OverflowMenu(content: @Composable () -> Unit, show: MutableState<Boolean>) {
        IconButton(
            onClick = { show.value = !show.value },
        ) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = "More",
            )
        }
        DropdownMenu(
            expanded = show.value,
            onDismissRequest = { show.value = false },
        ) {
            content()
        }
    }
}
