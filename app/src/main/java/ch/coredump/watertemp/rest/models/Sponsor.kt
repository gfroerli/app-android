package ch.coredump.watertemp.rest.models

/**
 * Gson Sponsor model.
 */
data class Sponsor(
    val id: Int,
    val name: String,
    val description: String?,
    val logoUrl: String?,
)