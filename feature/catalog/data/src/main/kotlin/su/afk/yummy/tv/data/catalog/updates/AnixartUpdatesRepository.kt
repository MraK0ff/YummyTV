package su.afk.yummy.tv.data.catalog.updates

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import su.afk.yummy.tv.data.catalog.network.YaniSearchApi
import su.afk.yummy.tv.domain.catalog.model.SearchFilters
import su.afk.yummy.tv.domain.catalog.updates.UpdatesFeedItem
import su.afk.yummy.tv.domain.catalog.updates.UpdatesFeedRepository
import java.util.concurrent.ConcurrentHashMap

class AnixartUpdatesRepository(
    private val anixartApi: AnixartUpdatesApi,
    private val yaniSearchApi: YaniSearchApi,
) : UpdatesFeedRepository {
    private val matchCache = ConcurrentHashMap<String, Int>()

    override suspend fun load(): List<UpdatesFeedItem> {
        val releases = buildList {
            for (page in 0..2) {
                val pageItems = anixartApi.getPage(page)
                if (pageItems.isEmpty()) break
                addAll(pageItems)
            }
        }.distinctBy(AnixartRelease::id)
            .sortedByDescending { it.lastUpdateAt ?: 0L }
            .take(60)

        val semaphore = Semaphore(4)
        return coroutineScope {
            releases.map { release ->
                async {
                    val key = "${release.id}:${release.year}:${normalize(release.title)}"
                    val animeId = matchCache[key] ?: semaphore.withPermit { findYummyId(release) }
                    if (animeId == null) return@async null
                    matchCache[key] = animeId
                    UpdatesFeedItem(
                        anixartId = release.id,
                        animeId = animeId,
                        title = release.title,
                        year = release.year,
                        episodesReleased = release.episodesReleased,
                        episodesTotal = release.episodesTotal,
                        lastUpdateAt = release.lastUpdateAt,
                        posterUrl = null,
                    )
                }
            }.awaitAll().filterNotNull()
        }
    }

    private suspend fun findYummyId(release: AnixartRelease): Int? {
        val exact = yaniSearchApi.search(
            query = release.title,
            filters = SearchFilters.EMPTY,
            limit = 10,
            offset = 0,
        ).filter { it.animeId != null && normalize(it.title) == normalize(release.title) }
        val yearMatches = exact.filter { release.year == null || it.year == release.year }
        return when {
            yearMatches.size == 1 -> yearMatches.single().animeId
            exact.size == 1 -> exact.single().animeId
            else -> null
        }
    }

    private fun normalize(value: String): String = value.lowercase().filter(Char::isLetterOrDigit)
}
