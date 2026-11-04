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
import android.util.Base64
import io.ktor.http.formData
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
    private val configurationStore: TorrServerConfigurationStore,
) {
    @Volatile private var configuration = configurationStore.load()

    fun configure(value: TorrServerConfiguration) {
        require(value.baseUrl.startsWith("http://") || value.baseUrl.startsWith("https://"))
        require(value.baseUrl.trimEnd('/').isNotBlank())
        require(value.username.none { it.code == 13 || it.code == 10 })
        require(value.password.none { it.code == 13 || it.code == 10 })
        val normalized = value.copy(baseUrl = value.baseUrl.trimEnd('/'))
        configuration = normalized
        configurationStore.saveBaseUrl(normalized.baseUrl)
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
        val safeName = fileName.substringAfterLast('/').substringAfterLast(92.toChar())
            .filterNot { it.isISOControl() || it == '"' }
            .take(120)
            .ifBlank { "torrent.torrent" }
        val response = client.post("${configuration.baseUrl}/torrent/upload") {
            auth()
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("file", bytes, Headers.build {
                            append(HttpHeaders.ContentDisposition, "form-data; name=\"file\"; filename=\"$safeName\"")
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

    /** Получает список файлов раздачи после загрузки метаданных TorrServer. */
    suspend fun getTorrentFiles(hash: String): List<TorrServerFile> {
        require(hash.matches(Regex("[A-Fa-f0-9]{20,64}"))) { "Invalid torrent hash" }
        val request = TorrServerGetRequest(action = "get", hash = hash)
        val response = client.post("${configuration.baseUrl}/torrents") {
            auth()
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(TorrServerGetRequest.serializer(), request))
        }
        if (response.status.value == 404) return emptyList()
        if (!response.status.isSuccess()) {
            throw IOException("TorrServer get torrent HTTP ${response.status.value}")
        }
        val root = runCatching { Json.parseToJsonElement(response.bodyAsText()).jsonObject }
            .getOrElse { throw IOException("Invalid TorrServer torrent response", it) }
        val files = root["file_stats"] as? kotlinx.serialization.json.JsonArray ?: return emptyList()
        return files.mapNotNull { element ->
            val file = runCatching { element.jsonObject }.getOrNull() ?: return@mapNotNull null
            val id = file["id"]?.jsonPrimitive?.content?.toIntOrNull() ?: return@mapNotNull null
            val filePath = file["path"]?.jsonPrimitive?.content?.takeIf(String::isNotBlank)
                ?: return@mapNotNull null
            val length = file["length"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
            TorrServerFile(id = id, path = filePath, sizeBytes = length)
        }.sortedBy(TorrServerFile::id)
    }
    /** URL потока TorrServer. Magnet никогда не передаётся напрямую ExoPlayer. */
    fun playbackUrl(hash: String, fileIndex: Int = 0): String {
        require(hash.matches(Regex("[A-Fa-f0-9]{20,64}")))
        require(fileIndex >= 0)
        return "${configuration.baseUrl}/play/$hash/$fileIndex"
    }

    fun playbackHeaders(): Map<String, String> = buildMap {
        if (configuration.username.isNotBlank()) {
            val token = "${configuration.username}:${configuration.password}"
            put("Authorization", "Basic " + Base64.encodeToString(token.toByteArray(Charsets.UTF_8), Base64.NO_WRAP))
        }
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
            header("Authorization", "Basic " + Base64.encodeToString(token.toByteArray(Charsets.UTF_8), Base64.NO_WRAP))
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

@Serializable
private data class TorrServerGetRequest(
    val action: String,
    val hash: String,
)

data class TorrServerFile(
    val id: Int,
    val path: String,
    val sizeBytes: Long,
)
