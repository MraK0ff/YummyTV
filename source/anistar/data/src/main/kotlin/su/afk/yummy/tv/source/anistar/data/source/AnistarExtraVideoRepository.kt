package su.afk.yummy.tv.source.anistar.data.source

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.domain.anime.repository.ExtraVideoRepository
import su.afk.yummy.tv.source.anistar.data.network.AnistarApi
import su.afk.yummy.tv.source.anistar.data.utils.findBestAnistarCandidate
import su.afk.yummy.tv.source.anistar.data.utils.parseAnistarPlaylist
import su.afk.yummy.tv.source.anistar.data.utils.toSourceUrl
import su.afk.yummy.tv.source.anistar.domain.model.AnistarEpisode
import su.afk.yummy.tv.source.anistar.domain.model.AnistarTarget
import su.afk.yummy.tv.source.anistar.domain.model.AnistarTitle
import su.afk.yummy.tv.source.anistar.domain.usecase.GetAnistarEpisodesUseCase
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ищет AniStar напрямую по основному и альтернативным названиям. MAL id не требуется:
 * серверный /availability больше не участвует в появлении озвучки в карточке.
 *
 * Кэш короткий, чтобы не долбить поиск при повторной перерисовке деталей, но быстро
 * подхватывать только что опубликованные серии. HLS URL здесь не кэшируются: токены
 * могут быстро истекать и запрашиваются отдельно при открытии плеера.
 */
@Singleton
internal class AnistarExtraVideoRepository @Inject constructor(
    private val api: AnistarApi,
    private val getEpisodes: GetAnistarEpisodesUseCase,
) : ExtraVideoRepository {

    override val sourceId: String = SOURCE_ID

    private data class CachedVideos(
        val expiresAtMs: Long,
        val videos: List<AnimeVideo>,
    )

    private val cache = ConcurrentHashMap<String, CachedVideos>()
    private val lookupMutex = Mutex()

    override suspend fun videos(details: AnimeDetails): List<AnimeVideo> {
        val names = (listOf(details.title) + details.otherTitles.orEmpty())
            .map(String::trim)
            .filter { it.length >= MIN_QUERY_LENGTH }
            .distinctBy(String::lowercase)
            .take(MAX_TITLE_ALIASES)
        if (names.isEmpty()) return emptyList()

        val cacheKey = (names + details.year.toString()).joinToString("|").lowercase()
        cache[cacheKey]?.takeIf { it.expiresAtMs > System.currentTimeMillis() }?.let { return it.videos }

        return lookupMutex.withLock {
            cache[cacheKey]?.takeIf { it.expiresAtMs > System.currentTimeMillis() }?.let { return@withLock it.videos }
            val result = findVideos(names, details.year)
            cache[cacheKey] = CachedVideos(System.currentTimeMillis() + CACHE_TTL_MS, result)
            result
        }
    }

    private suspend fun findVideos(names: List<String>, year: Int?): List<AnimeVideo> {
        for (name in names) {
            val searchPage = api.search(name)
            val candidate = findBestAnistarCandidate(searchPage.html, names, year) ?: continue
            val playerHtml = api.getPlayerPage(searchPage.mirror, candidate.newsId)
            val episodes = parseAnistarPlaylist(playerHtml)
                .map { AnistarEpisode(it.number) }
                .let { parsed ->
                    getEpisodes(AnistarTitle(candidate.newsId, searchPage.mirror, parsed))
                }
            if (episodes.isNotEmpty()) {
                return episodes.map { episode ->
                    val sourceUrl = AnistarTarget(candidate.newsId, episode.number).toSourceUrl()
                    AnimeVideo(
                        id = -(sourceUrl.hashCode() and Int.MAX_VALUE) - 1,
                        episode = episode.number.toString(),
                        dubbing = DUBBING_NAME,
                        player = PLAYER_NAME,
                        playerId = null,
                        iframeUrl = sourceUrl,
                        durationSeconds = null,
                    )
                }
            }
        }
        return emptyList()
    }

    private companion object {
        const val SOURCE_ID = "anistar"
        const val PLAYER_NAME = "AniStar"
        const val DUBBING_NAME = "AniStar"
        const val MIN_QUERY_LENGTH = 2
        const val MAX_TITLE_ALIASES = 4
        const val CACHE_TTL_MS = 30_000L
    }
}
