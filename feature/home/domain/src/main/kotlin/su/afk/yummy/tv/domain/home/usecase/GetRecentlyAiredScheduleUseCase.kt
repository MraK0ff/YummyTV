package su.afk.yummy.tv.domain.home.usecase

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import su.afk.yummy.tv.core.common.player.isSupportedPlayerUrl
import su.afk.yummy.tv.domain.home.model.HomeFeedItem
import su.afk.yummy.tv.domain.home.model.HomeFeedItemAction
import su.afk.yummy.tv.domain.home.utils.lastAiredSeconds
import su.afk.yummy.tv.domain.player.repository.PlayerSourceRepository
import su.afk.yummy.tv.domain.schedule.usecase.GetAnimeScheduleUseCase
import java.util.concurrent.CancellationException
import javax.inject.Inject

/**
 * Возвращает недавно вышедшие тайтлы, только если у последней вышедшей серии уже есть
 * поддерживаемый плеером видеоисточник. Дата расписания сама по себе не считается релизом.
 *
 * Запросы идут с ограниченной параллельностью через существующий кэш PlayerSourceRepository.
 * Временная ошибка источника скрывает только текущий элемент: следующий refresh повторит проверку.
 */
class GetRecentlyAiredScheduleUseCase @Inject constructor(
    private val getSchedule: GetAnimeScheduleUseCase,
    private val playerSourceRepository: PlayerSourceRepository,
) {
    suspend operator fun invoke(nowSeconds: Long): List<HomeFeedItem> {
        val threshold = nowSeconds - RECENT_WINDOW_SECONDS
        val candidates = getSchedule()
            .flatMap { it.items }
            .mapNotNull { item ->
                val lastAired = item.lastAiredSeconds(nowSeconds) ?: return@mapNotNull null
                item.takeIf { lastAired >= threshold }?.let { lastAired to it }
            }
            .sortedByDescending { (lastAired, _) -> lastAired }
            .distinctBy { (_, item) -> item.animeId }

        val available = coroutineScope {
            val semaphore = Semaphore(MAX_PARALLEL_SOURCE_CHECKS)
            candidates.map { candidate ->
                async {
                    semaphore.withPermit {
                        candidate.takeIf { (_, item) ->
                            hasPlayableEpisode(item.animeId, item.airedEpisodes)
                        }
                    }
                }
            }.awaitAll().filterNotNull()
        }

        return available.map { (lastAired, item) ->
            HomeFeedItem(
                id = item.animeId,
                title = item.title,
                description = "",
                poster = item.poster,
                rating = null,
                year = null,
                action = HomeFeedItemAction.OpenSeries(item.animeId),
                episodeNumber = item.airedEpisodes,
                airedAtSeconds = lastAired,
            )
        }
    }

    private suspend fun hasPlayableEpisode(animeId: Int, airedEpisodes: Int?): Boolean {
        if (airedEpisodes == null || airedEpisodes <= 0) return false
        return try {
            playerSourceRepository.getSources(animeId).videos.any { video ->
                video.episode.trim().toIntOrNull() == airedEpisodes &&
                    video.iframeUrl.isNotBlank() &&
                    video.iframeUrl.isSupportedPlayerUrl()
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // Не превращаем временный сетевой сбой в постоянное «видео отсутствует».
            false
        }
    }

    private companion object {
        const val RECENT_WINDOW_SECONDS = 14L * 24 * 60 * 60
        const val MAX_PARALLEL_SOURCE_CHECKS = 4
    }
}
