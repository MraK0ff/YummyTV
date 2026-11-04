package su.afk.yummy.tv.source.anistar.data.torrent

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.content.MultiPartFormDataContent
import io.ktor.http.contentType
import io.ktor.http.formData
import io.ktor.http.headersOf
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import su.afk.yummy.tv.core.network.di.UnauthenticatedJsonClient
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Клиент TorrServer API (POST /torrents, POST /torrent/upload, GET /play, GET /echo). */
@Singleton
class TorrServerClient @Inject constructor(
    @param:UnauthenticatedJsonClient private val client: HttpClient,
) {
    @Volatile private var configuration = TorrServerConfiguration()

    fun configure(value: TorrServerConfiguration) {
        require(value.baseUrl.startsWith("http://") || value.baseUrl.startsWith("https://"))
        require(value.baseUrl.trimEnd('/').isNotBlank())
        require(value.username.none { it == '\\r' || it == '\\n' })
        require(value.password.none { it == '\\r' || it == '\\n' })
        configuration = value.copy(baseUrl = value.baseUrl.trimEnd('/'))
    }

    suspend fun checkConnection(): String {
        val response = client.get("${configuration.baseUrl}/echo") { auth() }
        if (!response.status.isSuccess()) throw IOException("TorrServer echo HTTP ${response.status.value}")
        return response.bodyAsText()
    }

    /** Добавляет magnet или удалённый URL .torrent через JSON API. */
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
        return parseHash(response.bodyAsText())
    }

    /**
     * Передаёт локальный .torrent в официальный multipart endpoint.
     * Имя файла очищается от управляющих символов; размер ограничен 8 MiB.
     */
    suspend fun uploadTorrentFile(
        bytes: ByteArray,
        fileName: String = "torrent.torrent",
        title: String? = null,
        saveToDatabase: Boolean = true,
    ): String {
        require(bytes.isNotEmpty() && bytes.size <= MAX_TORRENT_FILE_BYTES) { "Invalid torrent file size" }
        val safeName = fileName.substringAfterLast('/').substringAfterLast('\\\\')
            .filterNot { it.isISOControl() || it == '"' }
            .take(120)
            .ifBlank { "torrent.torrent" }
        val response = client.post("${configuration.baseUrl}/torrent/upload") {
            auth()
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("file", bytes, Headers.build {
                            append(HttpHeaders.ContentDisposition, "filename=\"$safeName\"")
                            append(HttpHeaders.ContentType, "application/x-bittorrent")
                        })
                        if (!title.isNullOrBlank()) append("title", title)
                        if (saveToDatabase) append("save", "true")
                    },
                ),
            )
        }
        if (!response.status.isSuccess()) throw IOException("TorrServer upload HTTP ${response.status.value}")
        return parseHash(response.bodyAsText())
    }

    /** URL потока TorrServer. Magnet никогда не передаётся напрямую ExoPlayer. */
    fun playbackUrl(hash: String, fileIndex: Int = 0): String {
        require(hash.matches(Regex("[A-Fa-f0-9]{20,64}")))
        require(fileIndex >= 0)
        return "${configuration.baseUrl}/play/$hash/$fileIndex"
    }

    private fun parseHash(body: String): String {
        val json = runCatching { Json.parseToJsonElement(body) }.getOrElse {
            throw IOException("Invalid TorrServer response", it)
        }
        val obj = runCatching { json.jsonObject }.getOrNull()
            ?: throw IOException("TorrServer response is not an object")
        val hash = obj["hash"]?.jsonPrimitive?.content
            ?: obj["Hash"]?.jsonPrimitive?.content
            ?: obj["data"]?.let { data ->
                runCatching { data.jsonObject["hash"]?.jsonPrimitive?.content }.getOrNull()
            }
            ?: throw IOException("TorrServer response has no torrent hash")
        if (!hash.matches(Regex("[A-Fa-f0-9]{20,64}"))) {
            throw IOException("TorrServer returned an invalid torrent hash")
        }
        return hash
    }

    private fun io.ktor.client.request.HttpRequestBuilder.auth() {
        if (configuration.username.isNotBlank()) {
            val token = "${configuration.username}:${configuration.password}"
            header("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString(token.toByteArray()))
        }
    }

    private companion object {
        const val MAX_TORRENT_FILE_BYTES = 8 * 1024 * 1024
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
