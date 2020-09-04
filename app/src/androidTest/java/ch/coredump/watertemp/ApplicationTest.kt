package ch.coredump.watertemp

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import ch.coredump.watertemp.activities.MapActivity
import org.junit.Rule
import org.junit.Test

class ApplicationTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(MapActivity::class.java)

    @Test
    fun testMapActivity() {
        onView(withText("Gfrör.li: Water Temperatures"))
            .check(matches(isDisplayed()))
    }
}