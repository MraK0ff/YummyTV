package su.afk.yummy.tv.domain.catalog.updates

interface UpdatesFeedRepository {
    suspend fun load(): List<UpdatesFeedItem>
}
