package ch.coredump.watertemp.utils

import android.content.Context
import android.os.Handler
import android.view.View
import androidx.test.platform.app.InstrumentationRegistry
import ch.coredump.watertemp.R
import com.google.android.material.progressindicator.LinearProgressIndicator
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch

class ProgressCounterTest {
    private lateinit var context: Context
    private lateinit var handler: Handler
    private lateinit var view: LinearProgressIndicator

    @Before
    fun initialize() {
        // Get context
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.setTheme(androidx.appcompat.R.style.Theme_AppCompat)
        this.context = context

        // Initialize handler
        this.handler = Handler(context.mainLooper)

        // Initialize view
        val viewInitialized = CountDownLatch(1)
        handler.post {
            val view = LinearProgressIndicator(context)
            view.visibility = View.INVISIBLE
            this.view = view
            viewInitialized.countDown()
        }
        viewInitialized.await()
    }

    @Test
    fun testStartStop() {
        val progressCounter = ProgressCounter(this.view)
        Assert.assertEquals(this.view.visibility, View.INVISIBLE)
        progressCounter.increment()
        Assert.assertEquals(this.view.visibility, View.VISIBLE)
        progressCounter.decrement()
        Assert.assertEquals(this.view.visibility, View.INVISIBLE)
    }

    @Test
    fun testMixed() {
        val progressCounter = ProgressCounter(this.view)
        // count = 0
        Assert.assertEquals(this.view.visibility, View.INVISIBLE)
        progressCounter.decrement()
        // count = 0
        Assert.assertEquals(this.view.visibility, View.INVISIBLE)
        progressCounter.increment()
        // count = 1
        Assert.assertEquals(this.view.visibility, View.VISIBLE)
        progressCounter.increment()
        // count = 2
        Assert.assertEquals(this.view.visibility, View.VISIBLE)
        progressCounter.decrement()
        // count = 1
        Assert.assertEquals(this.view.visibility, View.VISIBLE)
        progressCounter.decrement()
        // count = 0
        Assert.assertEquals(this.view.visibility, View.INVISIBLE)
    }
}