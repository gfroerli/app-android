package ch.coredump.watertemp.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import ch.coredump.watertemp.R
import ch.coredump.watertemp.databinding.ActivityAboutBinding

private const val TAG = "AboutActivity"

class AboutActivity : AppCompatActivity() {
    // View bindings
    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the layout
        this.binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(this.binding.root)

        // Initialize the action bar
        setSupportActionBar(this.binding.aboutActionBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                this.finish()
                return true
            }
            else -> Log.w(TAG, "Selected unknown menu entry: $item")
        }
        return super.onOptionsItemSelected(item)
    }
}