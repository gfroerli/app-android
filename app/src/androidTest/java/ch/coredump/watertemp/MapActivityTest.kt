package ch.coredump.watertemp

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import ch.coredump.watertemp.activities.MapActivity
import org.junit.Rule
import org.junit.Test

@ExperimentalMaterialApi
class MapActivityTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MapActivity>()

    @Test
    fun testTitle() {
        val title = composeTestRule.activity.getString(R.string.activity_map)
        composeTestRule.onNodeWithText(title).assertExists("Title text not found")
    }
}