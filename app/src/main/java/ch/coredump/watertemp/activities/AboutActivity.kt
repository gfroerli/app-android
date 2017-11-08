package ch.coredump.watertemp.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import ch.coredump.watertemp.R
import kotlinx.android.synthetic.main.activity_about.*

class AboutActivity : AppCompatActivity() {

    val TAG = "AboutActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the layout
        setContentView(R.layout.activity_about)

        // Initialize the action bar
        setSupportActionBar(about_action_bar)
    }

}