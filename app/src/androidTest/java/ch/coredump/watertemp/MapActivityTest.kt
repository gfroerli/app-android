package ch.coredump.watertemp

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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

    @Test
    fun testAbout() {
        // Tap on menu button
        composeTestRule.onNodeWithContentDescription("More").performClick()

        // Pick "about" entry
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.action_about_this_app)).performClick()

        // Ensure about activity is opened
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.activity_about)).assertExists("Title text not found")
    }
}