package su.afk.yummy.tv.domain.details.torrent

/** Optional source integration for searching AniStar and handing selected torrents to TorrServer. */
interface AnimeTorrentSource {
    suspend fun search(title: String): List<AnimeTorrent>
    suspend fun add(torrent: AnimeTorrent): List<AnimeTorrentFile>
    fun playerSource(hash: String, fileIndex: Int): String
    fun configureServer(baseUrl: String, username: String, password: String)
    suspend fun checkServerConnection(): String
}

data class AnimeTorrent(
    val title: String,
    val pageUrl: String,
    val size: String? = null,
    val seeders: Int? = null,
    val leechers: Int? = null,
)

data class AnimeTorrentFile(
    val hash: String,
    val index: Int,
    val path: String,
    val sizeBytes: Long,
)
