package ch.coredump.watertemp.rest

import com.google.gson.JsonDeserializer
import com.google.gson.JsonParseException
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter

/**
 * Based on https://stackoverflow.com/a/36418842/284318
 */
internal object GsonHelper {
    val ZDT_DESERIALIZER: JsonDeserializer<ZonedDateTime> = JsonDeserializer { json, _, _ ->
        val jsonPrimitive = json.asJsonPrimitive
        try {
            // Parse ISO strings
            if (jsonPrimitive.isString) {
                return@JsonDeserializer ZonedDateTime.parse(jsonPrimitive.asString, DateTimeFormatter.ISO_DATE_TIME)
            }
            // Parse epoch timestamps
            if (jsonPrimitive.isNumber) {
                return@JsonDeserializer ZonedDateTime.ofInstant(Instant.ofEpochMilli(jsonPrimitive.asLong), ZoneId.of("UTC"))
            }
        } catch (e: RuntimeException) {
            throw JsonParseException("Unable to parse ZonedDateTime", e)
        }
        throw JsonParseException("Unable to parse ZonedDateTime")
    }
}