package ch.coredump.watertemp

class Config {
    companion object {
        const val SUPPORT_EMAIL = "gfroerli@coredump.ch"

        /** Sensors without a measurement within this many days are hidden. */
        const val SENSOR_MAX_AGE_DAYS = 3L
    }
}