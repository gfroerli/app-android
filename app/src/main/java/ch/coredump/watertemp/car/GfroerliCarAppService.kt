package ch.coredump.watertemp.car

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

/**
 * Entry point for the Android Auto integration. The car host binds to this service
 * (declared in the manifest with the POI category).
 */
class GfroerliCarAppService : CarAppService() {

    // The allowlist resource is technically private, but using it is the documented
    // way to restrict binding to the official Android Auto hosts.
    @SuppressLint("PrivateResource")
    override fun createHostValidator(): HostValidator =
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
        } else {
            // Only allow the official Android Auto hosts to bind
            HostValidator.Builder(applicationContext)
                .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)
                .build()
        }

    override fun onCreateSession(): Session = GfroerliCarSession()
}
