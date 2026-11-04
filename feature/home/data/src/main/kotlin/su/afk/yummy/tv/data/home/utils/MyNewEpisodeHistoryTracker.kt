package su.afk.yummy.tv.data.home.utils

import su.afk.yummy.tv.core.model.anime.AnimePoster
import su.afk.yummy.tv.core.storage.home.HomeMyNewEpisodeEntry
import su.afk.yummy.tv.domain.home.model.HomeFeedItem
import su.afk.yummy.tv.domain.home.model.HomeFeedItemAction

/**
 * Keeps one newest aired episode per anime. Initial data establishes a baseline; only a larger
 * episode number advances an existing card and changes its sort position.
 */
internal class MyNewEpisodeHistoryTracker {

    fun merge(
        recentlyAired: List<HomeFeedItem>,
        history: List<HomeMyNewEpisodeEntry>,
        libraryAnimeIds: Set<Int>,
        nowSeconds: Long,
    ): List<HomeMyNewEpisodeEntry> {
        val candidates = recentlyAired
            .asSequence()
            .filter { it.id in libraryAnimeIds }
            .mapNotNull { item ->
                val episode = item.episodeNumber?.takeIf { it > 0 } ?: return@mapNotNull null
                val airedAt = item.airedAtSeconds ?: return@mapNotNull null
                item.copy(episodeNumber = episode, airedAtSeconds = airedAt)
            }
            .groupBy { it.id }
            .mapValues { (_, items) ->
                items.maxWithOrNull(
                    compareBy<HomeFeedItem> { it.episodeNumber ?: 0 }
                        .thenBy { it.airedAtSeconds ?: 0L }
                        .thenBy { it.title }
                        .thenBy { it.poster?.mega.orEmpty() },
                )
            }
            .mapNotNull { it.value }

        if (candidates.isEmpty()) return history.sortedBy { it.position }

        val initialSnapshot = history.isEmpty()
        val merged = history.associateBy { it.animeId }.toMutableMap()
        candidates.forEach { item ->
            val previous = merged[item.id]
            val episode = requireNotNull(item.episodeNumber)
            when {
                previous == null -> {
                    merged[item.id] = item.toHistoryEntry(
                        firstSeenAt = nowSeconds,
                        lastUpdatedAt = if (initialSnapshot) 0L else nowSeconds,
                        position = if (initialSnapshot) merged.size else -1,
                    )
                }
                episode > previous.episodeNumber -> {
                    merged[item.id] = item.toHistoryEntry(
                        firstSeenAt = previous.firstSeenAt,
                        lastUpdatedAt = nowSeconds,
                        position = previous.position,
                    )
                }
                else -> Unit
            }
        }

        return merged.values.sortedWith(
            compareByDescending<HomeMyNewEpisodeEntry> { it.lastUpdatedAt > 0L }
                .thenByDescending { it.lastUpdatedAt }
                .thenBy { it.position }
                .thenBy { it.animeId },
        ).mapIndexed { index, entry -> entry.copy(position = index) }
    }
}

private fun HomeFeedItem.toHistoryEntry(
    firstSeenAt: Long,
    lastUpdatedAt: Long,
    position: Int,
): HomeMyNewEpisodeEntry {
    val itemPoster = poster
    return HomeMyNewEpisodeEntry(
        animeId = id,
        title = title,
        description = description,
        episodeNumber = requireNotNull(episodeNumber),
        airedAtSeconds = requireNotNull(airedAtSeconds),
        posterSmallUrl = itemPoster?.small,
        posterMediumUrl = itemPoster?.medium,
        posterBigUrl = itemPoster?.big,
        posterFullsizeUrl = itemPoster?.fullsize,
        posterMegaUrl = itemPoster?.mega,
        firstSeenAt = firstSeenAt,
        lastUpdatedAt = lastUpdatedAt,
        position = position,
    )
}

internal fun HomeMyNewEpisodeEntry.toHomeFeedItem(): HomeFeedItem =
    HomeFeedItem(
        id = animeId,
        title = title,
        description = description,
        poster = if (
            posterSmallUrl != null || posterMediumUrl != null || posterBigUrl != null ||
            posterFullsizeUrl != null || posterMegaUrl != null
        ) {
            AnimePoster(
                small = posterSmallUrl,
                medium = posterMediumUrl,
                big = posterBigUrl,
                fullsize = posterFullsizeUrl,
                mega = posterMegaUrl,
            )
        } else {
            null
        },
        rating = null,
        year = null,
        action = HomeFeedItemAction.OpenSeries(animeId),
        episodeNumber = episodeNumber,
        airedAtSeconds = airedAtSeconds,
    )
