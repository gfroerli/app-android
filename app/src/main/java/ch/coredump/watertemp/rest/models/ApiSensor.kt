package ch.coredump.watertemp.rest.models

/**
 * Gson Sensor model.
 */
data class ApiSensor(
    val id: Int,
    val deviceName: String,
    val caption: String?,
    val latitude: Double?,
    val longitude: Double?,
    val latestTemperature: Float?,
    val latestMeasurementAt: Long?, // TODO: Datetime?
    val sponsorId: Int?,
)