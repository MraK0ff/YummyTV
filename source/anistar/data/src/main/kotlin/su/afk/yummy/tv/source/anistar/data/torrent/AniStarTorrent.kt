package su.afk.yummy.tv.source.anistar.data.torrent

/** Раздача из каталога AniStar. Сетевые DTO и UI-модели не смешиваются. */
data class AniStarTorrent(
    val title: String,
    val url: String,
    val size: String?,
    val seeders: Int?,
    val leechers: Int?,
    val downloads: Int? = null,
    val magnetUri: String? = null,
    val torrentFileUrl: String? = null,
)

internal data class AniStarTorrentPage(
    val title: String?,
    val magnetUri: String?,
    val torrentFileUrl: String?,
)
