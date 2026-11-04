package su.afk.yummy.tv.data.home.utils

import java.util.Locale
import su.afk.yummy.tv.core.storage.home.HomeNewReleaseEntry
import su.afk.yummy.tv.data.home.dto.YaniVideoDto

/**
 * Merges /feed new_videos into durable anime history. When publication order is unknown,
 * alternate uploads are selected by stable dubbing, player, and video-id metadata ordering.
 */
internal class YaniNewReleaseTracker {

    fun merge(
        videos: List<YaniVideoDto>,
        history: List<HomeNewReleaseEntry>,
        now: Long,
    ): List<HomeNewReleaseEntry> {
        if (videos.isEmpty()) return history.sortedBy { it.position }

        val candidates = videos.mapIndexedNotNull { index, video ->
            val animeId = video.animeId?.takeIf { it > 0 } ?: return@mapIndexedNotNull null
            val episodeNumber = video.episodeTitle.toRegularEpisodeNumber()
                ?: return@mapIndexedNotNull null
            val title = video.title.takeIf(String::isNotBlank) ?: return@mapIndexedNotNull null
            Candidate(index, animeId, episodeNumber, video, title)
        }.groupBy { it.animeId }

        if (candidates.isEmpty()) return history.sortedBy { it.position }

        val previousById = history.associateBy { it.animeId }
        val isInitialSnapshot = history.isEmpty()
        val merged = previousById.toMutableMap()

        candidates.forEach { (animeId, items) ->
            // Highest regular episode wins. API publication ordering is not assumed: alternate uploads
            // use a stable dubbing/player/video-id ordering instead.
            val newest = items.sortedWith(
                compareByDescending<Candidate> { it.episodeNumber }
                    .thenBy { it.video.dubTitle.orEmpty().lowercase(Locale.ROOT) }
                    .thenBy { it.video.playerTitle.orEmpty().lowercase(Locale.ROOT) }
                    .thenBy { it.video.videoId ?: Int.MAX_VALUE }
                    .thenBy { it.video.animeUrl.orEmpty() }
                    .thenBy { it.video.title }
                    .thenBy { it.video.description }
                    .thenBy { it.video.episodeTitle.orEmpty() }
                    .thenBy { it.video.poster?.big.orEmpty() }
                    .thenBy { it.video.poster?.medium.orEmpty() }
                    .thenBy { it.video.poster?.small.orEmpty() }
                    .thenBy { it.index },
            ).firstOrNull() ?: return@forEach
            val previous = previousById[animeId]

            when {
                previous == null -> {
                    merged[animeId] = newest.toEntry(
                        now = now,
                        firstSeenAt = now,
                        lastUpdatedAt = if (isInitialSnapshot) 0L else now,
                        position = if (isInitialSnapshot) merged.size else -1,
                    )
                }
                newest.episodeNumber > previous.episodeNumber -> {
                    merged[animeId] = newest.toEntry(
                        now = now,
                        firstSeenAt = previous.firstSeenAt,
                        lastUpdatedAt = now,
                        position = previous.position,
                    )
                }
                else -> {
                    // A repost, alternate dub, or stale feed snapshot must not replace the
                    // selected upload or move the card. Only the detection timestamp changes.
                    merged[animeId] = previous.copy(lastDetectedAt = now)
                }
            }
        }

        return merged.values.sortedWith(
            compareByDescending<HomeNewReleaseEntry> { it.lastUpdatedAt > 0L }
                .thenByDescending { it.lastUpdatedAt }
                .thenBy { it.position }
                .thenBy { it.animeId },
        ).mapIndexed { index, entry -> entry.copy(position = index) }
    }

    private data class Candidate(
        val index: Int,
        val animeId: Int,
        val episodeNumber: Int,
        val video: YaniVideoDto,
        val title: String,
    ) {
        fun toEntry(
            now: Long,
            firstSeenAt: Long,
            lastUpdatedAt: Long,
            position: Int,
        ): HomeNewReleaseEntry = HomeNewReleaseEntry(
            animeId = animeId,
            title = title,
            animeUrl = video.animeUrl.orEmpty(),
            description = video.description,
            episodeNumber = episodeNumber,
            episodeTitle = video.episodeTitle.orEmpty(),
            videoId = video.videoId,
            dubTitle = video.dubTitle,
            playerTitle = video.playerTitle,
            posterSmallUrl = video.poster?.small,
            posterMediumUrl = video.poster?.medium,
            posterBigUrl = video.poster?.big,
            posterFullsizeUrl = video.poster?.fullsize,
            posterMegaUrl = video.poster?.mega,
            firstSeenAt = firstSeenAt,
            lastDetectedAt = now,
            lastUpdatedAt = lastUpdatedAt,
            position = position,
        )
    }
}

/**
 * Only integral episode numbers are regular episodes. Decimal specials (e.g. 7.5), OVA, SP,
 * movies, and arbitrary text are intentionally excluded rather than rounded into a series.
 */
internal fun String?.toRegularEpisodeNumber(): Int? {
    val value = this?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val episodePattern = Regex(
        """^\d+(?:[.,]\d+)?(?:\s*(?:серия|серии|эпизод|episode|ep\.?)?)?$""",
        RegexOption.IGNORE_CASE,
    )
    if (!value.matches(episodePattern)) return null
    val numeric = Regex("""^\d+(?:[.,]\d+)?""").find(value)?.value ?: return null
    val decimal = numeric.toBigDecimalOrNull() ?: return null
    if (decimal.stripTrailingZeros().scale() > 0) return null
    return runCatching { decimal.intValueExact() }.getOrNull()?.takeIf { it > 0 }
}
