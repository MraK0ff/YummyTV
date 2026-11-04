package su.afk.yummy.tv.data.catalog.updates

import io.ktor.client.call.body
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import su.afk.yummy.tv.core.network.yani.YaniHttpClientProvider
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class AnixartRelease(
    val id: Long,
    val title: String,
    val year: Int?,
    val episodesReleased: Int?,
    val episodesTotal: Int?,
    val lastUpdateAt: Long?,
)

class AnixartUpdatesApi(private val clientProvider: YaniHttpClientProvider) {
    suspend fun getPage(page: Int): List<AnixartRelease> {
        val response = clientProvider.get().post("$BASE_URL/filter/$page") {
            parameter("extended_mode", "true")
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("sort", JsonPrimitive(0)) })
        }.body<JsonElement>()

        return response.releaseObjects()
            .mapNotNull { obj ->
                val id = obj.text("id")?.toLongOrNull() ?: return@mapNotNull null
                val title = obj.text("title")?.takeIf(String::isNotBlank) ?: return@mapNotNull null
                AnixartRelease(
                    id = id,
                    title = title,
                    year = obj.text("year")?.toIntOrNull(),
                    episodesReleased = obj.text("episodes_released")?.toIntOrNull(),
                    episodesTotal = obj.text("episodes_total")?.toIntOrNull(),
                    lastUpdateAt = obj.text("last_update_date")?.timestamp(),
                )
            }.distinctBy(AnixartRelease::id)
    }

    private fun JsonElement.releaseObjects(): List<JsonObject> = when (this) {
        is JsonObject -> (if (containsKey("id") && containsKey("title")) listOf(this) else emptyList()) +
            values.flatMap { value -> value.releaseObjects() }
        else -> runCatching { jsonArray.flatMap { value -> value.releaseObjects() } }.getOrDefault(emptyList())
    }

    private fun JsonObject.text(key: String): String? =
        (get(key) as? JsonPrimitive)?.contentOrNull

    private fun String.timestamp(): Long? {
        toLongOrNull()?.let { return if (it < 10_000_000_000L) it * 1000 else it }
        runCatching { Instant.parse(this).toEpochMilli() }.getOrNull()?.let { return it }
        return runCatching {
            LocalDateTime.parse(this, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }.getOrNull()
    }

    private companion object {
        const val BASE_URL = "https://api-s.anixsekai.com"
    }
}
