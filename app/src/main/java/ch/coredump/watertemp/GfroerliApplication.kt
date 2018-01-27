package ch.coredump.watertemp

import android.app.Application
import com.jakewharton.threetenabp.AndroidThreeTen

class GfroerliApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidThreeTen.init(this)
    }
}