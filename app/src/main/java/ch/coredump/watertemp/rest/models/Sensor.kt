package ch.coredump.watertemp.rest.models

/**
 * Gson Sensor model.
 */
data class Sensor(
    val id: Int,
    val deviceName: String,
    val caption: String?,
    val latitude: Double?,
    val longitude: Double?,
    val latestTemperature: Double?,
    val latestMeasurementAt: Long?,
    val sponsorId: Int?,
)