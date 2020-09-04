package ch.coredump.watertemp.utils

import android.view.View
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class ProgressCounterTest {
    private lateinit var view: View
    private lateinit var progressCounter: ProgressCounter

    @Before
    fun initialize() {
        // Create view mock
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        this.view = View(context)

        // Initialize view as invisible
        view.visibility = View.INVISIBLE

        // Initialize progress counter
        this.progressCounter = ProgressCounter(this.view)
    }

    @Test
    fun testStartStop() {
        Assert.assertEquals(this.view.visibility, View.INVISIBLE)
        this.progressCounter.start()
        Assert.assertEquals(this.view.visibility, View.VISIBLE)
        this.progressCounter.stop()
        Assert.assertEquals(this.view.visibility, View.INVISIBLE)
    }

    @Test
    fun testMixed() {
        // count = 0
        Assert.assertEquals(this.view.visibility, View.INVISIBLE)
        this.progressCounter.stop()
        // count = 0
        Assert.assertEquals(this.view.visibility, View.INVISIBLE)
        this.progressCounter.start()
        // count = 1
        Assert.assertEquals(this.view.visibility, View.VISIBLE)
        this.progressCounter.start()
        // count = 2
        Assert.assertEquals(this.view.visibility, View.VISIBLE)
        this.progressCounter.stop()
        // count = 1
        Assert.assertEquals(this.view.visibility, View.VISIBLE)
        this.progressCounter.stop()
        // count = 0
        Assert.assertEquals(this.view.visibility, View.INVISIBLE)
    }
}