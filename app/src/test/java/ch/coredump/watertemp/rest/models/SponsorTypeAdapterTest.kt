package ch.coredump.watertemp.rest.models

import com.google.gson.GsonBuilder
import org.junit.Assert
import org.junit.Test

class SponsorTypeAdapterTest {
    private val gson = GsonBuilder()
        .registerTypeAdapter(SponsorType::class.java, SponsorTypeAdapter())
        .create()

    @Test
    fun `deserialize sponsor type`() {
        val result = gson.fromJson("\"sponsor\"", SponsorType::class.java)
        Assert.assertEquals(SponsorType.Sponsor, result)
    }

    @Test
    fun `deserialize partner type`() {
        val result = gson.fromJson("\"partner\"", SponsorType::class.java)
        Assert.assertEquals(SponsorType.Partner, result)
    }

    @Test
    fun `deserialize public_data_provider type`() {
        val result = gson.fromJson("\"public_data_provider\"", SponsorType::class.java)
        Assert.assertEquals(SponsorType.PublicDataProvider, result)
    }

    @Test
    fun `deserialize unknown type preserves value`() {
        val result = gson.fromJson("\"some_future_type\"", SponsorType::class.java)
        Assert.assertEquals(SponsorType.Unknown("some_future_type"), result)
    }
}