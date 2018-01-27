package ch.coredump.watertemp.rest.models

/**
 * Gson Sensor model.
 */
data class Sensor(
    val id: Int,
    val deviceName: String,
    val caption: String?,
    val sponsorId: Int?,
    val latitude: Double?,
    val longitude: Double?,
    val lastMeasurement: Measurement?
)