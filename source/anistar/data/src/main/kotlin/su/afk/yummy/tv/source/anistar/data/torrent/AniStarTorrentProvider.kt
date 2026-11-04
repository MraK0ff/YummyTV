package su.afk.yummy.tv.source.anistar.data.torrent

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.readAvailable
import java.io.ByteArrayOutputStream
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.network.di.UnauthenticatedJsonClient
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Загружает каталог и разрешает magnet/.torrent отдельно от списка. */
@Singleton
class AniStarTorrentProvider @Inject constructor(
    @param:UnauthenticatedJsonClient private val client: HttpClient,
    private val headers: AniStarTorrentHeaders,
) {
    suspend fun getTorrents(listUrl: String): List<AniStarTorrent> = withContext(Dispatchers.IO) {
        val response = client.get(listUrl) {
            headers.snapshot().forEach { (name, value) -> header(name, value) }
        }
        if (!response.status.isSuccess()) throw IOException("AniStar HTTP ${response.status.value}")
        AniStarTorrentParser.parseList(response.bodyAsText(), listUrl)
    }


    suspend fun downloadTorrentFile(torrent: AniStarTorrent): ByteArray = withContext(Dispatchers.IO) {
        val url = torrent.torrentFileUrl ?: throw IOException("Torrent file URL is missing")
        val response = client.get(url) {
            headers.snapshot().forEach { (name, value) -> header(name, value) }
        }
        if (!response.status.isSuccess()) throw IOException("AniStar torrent download HTTP " + response.status.value)
        val declaredLength = response.headers[io.ktor.http.HttpHeaders.ContentLength]?.toLongOrNull()
        if (declaredLength != null && declaredLength > MAX_TORRENT_FILE_BYTES) {
            throw IOException("Torrent file exceeds 8 MiB limit")
        }

        val channel = response.bodyAsChannel()
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        while (true) {
            val count = channel.readAvailable(buffer, 0, buffer.size)
            if (count == -1) break
            if (count == 0) continue
            if (output.size() + count > MAX_TORRENT_FILE_BYTES) {
                throw IOException("Torrent file exceeds 8 MiB limit")
            }
            output.write(buffer, 0, count)
        }
        val bytes = output.toByteArray()
        if (bytes.isEmpty()) throw IOException("Empty torrent file")
        bytes
    }

    suspend fun resolve(torrent: AniStarTorrent): AniStarTorrent = withContext(Dispatchers.IO) {
        val response = client.get(torrent.url) {
            headers.snapshot().forEach { (name, value) -> header(name, value) }
        }
        if (!response.status.isSuccess()) throw IOException("AniStar torrent page HTTP ${response.status.value}")
        val page = AniStarTorrentParser.parseTorrentPage(response.bodyAsText(), response.request.url.toString())
        if (page.magnetUri == null && page.torrentFileUrl == null) {
            throw IOException("AniStar page contains neither magnet nor .torrent link")
        }
        torrent.copy(
            title = page.title?.takeIf(String::isNotBlank) ?: torrent.title,
            magnetUri = page.magnetUri,
            torrentFileUrl = page.torrentFileUrl,
        )
    }
    private companion object { const val MAX_TORRENT_FILE_BYTES = 8 * 1024 * 1024 }
}

/**
 * Заголовки хранятся в runtime-конфигурации, чтобы Cookie не попадали в репозиторий.
 * Settings/DataStore UI может обновлять их через update().
 */
@Singleton
class AniStarTorrentHeaders @Inject constructor() {
    @Volatile private var values: Map<String, String> = mapOf("User-Agent" to "YummyTV Android")

    fun update(headers: Map<String, String>) {
        values = headers.filter { (key, value) ->
            key.isNotBlank() && value.isNotBlank() &&
                !key.contains("\r") && !key.contains("\n") &&
                !value.contains("\r") && !value.contains("\n")
        }
    }

    fun snapshot(): Map<String, String> = values
}
