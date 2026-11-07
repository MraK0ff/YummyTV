package su.afk.yummy.tv.domain.home.repository

import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.model.anime.AnimeWatchProgress
import su.afk.yummy.tv.domain.home.model.ContinueWatchingProgressMigration
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.model.HomeFeed
import su.afk.yummy.tv.domain.home.model.HomeFeedItem

interface HomeFeedRepository {
    suspend fun getHomeFeed(): HomeFeed
    suspend fun getCachedHomeFeed(): HomeFeed?
    suspend fun refreshHomeFeed(): HomeFeed

    /**
     * Merges recently aired episodes into durable per-anime history for MY_NEW_EPISODES.
     * Existing entries remain available when they disappear from the recent schedule window.
     */
    suspend fun trackMyNewEpisodes(
        recentlyAired: List<HomeFeedItem>,
        libraryAnimeIds: Set<Int>,
        nowSeconds: Long,
    ): List<HomeFeedItem>
    suspend fun removeCachedContinueWatching(animeId: Int)
    suspend fun getContinueWatchingVideoIds(animeId: Int): List<Int>
    suspend fun migrateContinueWatchingProgress(migration: ContinueWatchingProgressMigration)
    fun observeContinueWatching(): Flow<List<HomeContinueWatchingItem>>

    /** Вся локальная история прогресса, по записи на серию, без фильтров «Продолжить просмотр». */
    fun observeWatchHistory(): Flow<List<HomeContinueWatchingItem>>

    /**
     * Серверный прогресс (`watched` из `/videos`) тайтлов, чьи детали уже открывали: тот же источник,
     * что у списка серий. Тайтлов без кэша здесь нет.
     */
    fun observeServerWatchProgress(): Flow<List<AnimeWatchProgress>>

    /**
     * Максимальный номер доступной серии по тайтлам из кэша `/videos`: id тайтла → номер.
     * Тайтлов без кэша здесь нет, поэтому для них доступность следующей серии неизвестна.
     */
    fun observeAvailableEpisodes(): Flow<Map<Int, Int>>

    /** Ручные удаления из «Продолжить просмотр»: id тайтла → момент удаления (мс). */
    fun observeContinueWatchingSuppressions(): Flow<Map<Int, Long>>

    /** Настройка «Следующая серия»: после досмотренной серии предлагать следующую. */
    fun observeSuggestNextEpisodeOnWatched(): Flow<Boolean>

    /**
     * Просмотренные серии по тайтлам: id тайтла → номера серий. Номера нормализованы, потому что
     * озвучки присылают их по-разному («01» и «1» — одна и та же серия).
     */
    fun observeWatchedEpisodes(): Flow<Map<Int, Set<Int>>>
}
