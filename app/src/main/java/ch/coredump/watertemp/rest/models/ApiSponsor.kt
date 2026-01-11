package ch.coredump.watertemp.rest.models

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

sealed class SponsorType {
    data object Sponsor : SponsorType()
    data object Partner : SponsorType()
    data object PublicDataProvider : SponsorType()
    data class Unknown(val value: String) : SponsorType()
}

class SponsorTypeAdapter : JsonDeserializer<SponsorType> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): SponsorType {
        return when (val value = json.asString) {
            "sponsor" -> SponsorType.Sponsor
            "partner" -> SponsorType.Partner
            "public_data_provider" -> SponsorType.PublicDataProvider
            else -> SponsorType.Unknown(value)
        }
    }
}

/**
 * Gson Sponsor model.
 */
data class ApiSponsor(
    val id: Int,
    val name: String,
    val description: String?,
    val logoUrl: String?,
    val sponsorType: SponsorType,
)