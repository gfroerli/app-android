package ch.coredump.watertemp.rest.models

/**
 * Gson Sensor model with details.
 */
data class ApiSensorDetails(
    val id: Int,
    val deviceName: String,
    val caption: String?,
    val latitude: Double?,
    val longitude: Double?,
    val latestTemperature: Double?,
    val latestMeasurementAt: Long?,
    val sponsorId: Int?,
    val minimumTemperature: Double?,
    val maximumTemperature: Double?,
    val averageTemperature: Double?,
)