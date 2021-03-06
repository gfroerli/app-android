package ch.coredump.watertemp.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import ch.coredump.watertemp.R
import kotlinx.android.synthetic.main.activity_about.*

private const val TAG = "AboutActivity"

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the layout
        setContentView(R.layout.activity_about)

        // Initialize the action bar
        setSupportActionBar(about_action_bar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.home -> {
                this.finish()
                return true
            }
            else -> Log.w(TAG, "Selected unknown menu entry: $item")
        }
        return super.onOptionsItemSelected(item)
    }
}