package su.afk.yummy.tv.source.anistar.data.torrent

import kotlinx.coroutines.delay
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Orchestrates AniStar discovery -> TorrServer ingestion -> file selection -> player reference. */
@Singleton
class AniStarTorrentPlaybackManager @Inject constructor(
    private val aniStar: AniStarTorrentProvider,
    private val torrServer: TorrServerClient,
) {
    suspend fun search(title: String): List<AniStarTorrent> {
        require(title.isNotBlank()) { "Anime title is required" }
        return aniStar.searchTorrents(title)
    }

    suspend fun add(torrent: AniStarTorrent): AddedAniStarTorrent {
        val resolved = aniStar.resolve(torrent)
        val source = resolved.magnetUri ?: resolved.torrentFileUrl
            ?: throw IOException("AniStar result has no magnet or torrent file")
        val hash = torrServer.add(source, resolved.title)
        repeat(METADATA_ATTEMPTS) {
            val files = torrServer.getTorrentFiles(hash)
            if (files.isNotEmpty()) return AddedAniStarTorrent(hash, files)
            delay(METADATA_RETRY_DELAY_MS)
        }
        throw IOException("TorrServer did not return torrent files before timeout")
    }

    fun playerSource(hash: String, fileIndex: Int): String {
        require(hash.matches(HASH_PATTERN)) { "Invalid torrent hash" }
        require(fileIndex >= 0) { "Invalid torrent file index" }
        return "$PLAYER_SOURCE_PREFIX$hash/$fileIndex"
    }

    fun configureServer(configuration: TorrServerConfiguration) = torrServer.configure(configuration)
    suspend fun checkServerConnection(): String = torrServer.checkConnection()

    private companion object {
        const val PLAYER_SOURCE_PREFIX = "source://torrserver/"
        const val METADATA_ATTEMPTS = 15
        const val METADATA_RETRY_DELAY_MS = 1_000L
        val HASH_PATTERN = Regex("[A-Fa-f0-9]{20,64}")
    }
}

data class AddedAniStarTorrent(
    val hash: String,
    val files: List<TorrServerFile>,
)
