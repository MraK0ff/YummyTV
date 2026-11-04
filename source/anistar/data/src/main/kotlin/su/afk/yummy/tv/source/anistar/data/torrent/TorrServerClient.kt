package su.afk.yummy.tv.source.anistar.data.torrent

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.isSuccess
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import su.afk.yummy.tv.core.network.di.UnauthenticatedJsonClient
import java.io.IOException
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/** Клиент TorrServer API (swagger: POST /torrents, GET /stream, GET /echo). */
@Singleton
class TorrServerClient @Inject constructor(
    @param:UnauthenticatedJsonClient private val client: HttpClient,
) {
    @Volatile private var configuration = TorrServerConfiguration()

    fun configure(value: TorrServerConfiguration) {
        require(value.baseUrl.startsWith("http://") || value.baseUrl.startsWith("https://"))
        require(value.baseUrl.trimEnd('/').isNotBlank())
        require(value.username.none { it == '\r' || it == '\n' })
        configuration = value.copy(baseUrl = value.baseUrl.trimEnd('/'))
    }

    suspend fun checkConnection(): String {
        val response = client.get("${configuration.baseUrl}/echo") { auth() }
        if (!response.status.isSuccess()) throw IOException("TorrServer echo HTTP ${response.status.value}")
        return response.bodyAsText()
    }

    /** Добавляет magnet или URL .torrent. Ответ TorrServer содержит hash, необходимый для /play. */
    suspend fun add(link: String, title: String? = null): String {
        require(link.startsWith("magnet:") || link.endsWith(".torrent", ignoreCase = true) ||
            link.startsWith("http://") || link.startsWith("https://")) { "Unsupported torrent source" }
        val body = TorrServerAddRequest(action = "add", link = link, title = title.orEmpty())
        val response = client.post("${configuration.baseUrl}/torrents") {
            auth()
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(TorrServerAddRequest.serializer(), body))
        }
        if (!response.status.isSuccess()) throw IOException("TorrServer add HTTP ${response.status.value}")
        val json = runCatching { Json.parseToJsonElement(response.bodyAsText()).jsonObject }.getOrElse {
            throw IOException("Invalid TorrServer add response", it)
        }
        val hash = json["hash"]?.jsonPrimitive?.content
            ?: json["data"]?.jsonObject?.get("hash")?.jsonPrimitive?.content
            ?: throw IOException("TorrServer response has no torrent hash")
        return hash
    }

    /** URL потока TorrServer. Поток отдаёт сервер; magnet никогда не передаётся ExoPlayer. */
    fun playbackUrl(hash: String, fileIndex: Int = 0): String {
        require(hash.matches(Regex("[A-Fa-f0-9]{20,64}")))
        require(fileIndex >= 0)
        return "${configuration.baseUrl}/play/$hash/$fileIndex"
    }

    private fun io.ktor.client.request.HttpRequestBuilder.auth() {
        if (configuration.username.isNotBlank()) {
            val token = "${configuration.username}:${configuration.password}"
            header("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString(token.toByteArray()))
        }
    }
}

data class TorrServerConfiguration(
    val baseUrl: String = "http://127.0.0.1:8090",
    val username: String = "",
    val password: String = "",
)

@Serializable
private data class TorrServerAddRequest(
    val action: String,
    val link: String,
    val title: String = "",
)
