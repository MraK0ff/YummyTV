package su.afk.yummy.tv.domain.catalog.updates

data class UpdatesFeedItem(
    val anixartId: Long,
    val animeId: Int,
    val title: String,
    val year: Int?,
    val episodesReleased: Int?,
    val episodesTotal: Int?,
    val lastUpdateAt: Long?,
    val posterUrl: String?,
)
