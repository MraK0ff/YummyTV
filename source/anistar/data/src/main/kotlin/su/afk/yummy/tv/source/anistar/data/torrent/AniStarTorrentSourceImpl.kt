package su.afk.yummy.tv.source.anistar.data.torrent

import su.afk.yummy.tv.domain.details.torrent.AnimeTorrent
import su.afk.yummy.tv.domain.details.torrent.AnimeTorrentFile
import su.afk.yummy.tv.domain.details.torrent.AnimeTorrentSource
import javax.inject.Inject

class AniStarTorrentSourceImpl @Inject constructor(
    private val manager: AniStarTorrentPlaybackManager,
) : AnimeTorrentSource {
    override suspend fun search(title: String): List<AnimeTorrent> =
        manager.search(title).map { it.toDomain() }

    override suspend fun add(torrent: AnimeTorrent): List<AnimeTorrentFile> {
        val added = manager.add(torrent.toData())
        return added.files.map { file ->
            AnimeTorrentFile(
                hash = added.hash,
                index = file.id,
                path = file.path,
                sizeBytes = file.sizeBytes,
            )
        }
    }

    override fun playerSource(hash: String, fileIndex: Int): String =
        manager.playerSource(hash, fileIndex)

    override fun configureServer(baseUrl: String, username: String, password: String) {
        manager.configureServer(TorrServerConfiguration(baseUrl, username, password))
    }

    override suspend fun checkServerConnection(): String = manager.checkServerConnection()

    private fun AniStarTorrent.toDomain() = AnimeTorrent(
        title = title,
        pageUrl = url,
        size = size,
        seeders = seeders,
        leechers = leechers,
    )

    private fun AnimeTorrent.toData() = AniStarTorrent(
        title = title,
        url = pageUrl,
        size = size,
        seeders = seeders,
        leechers = leechers,
    )
}
