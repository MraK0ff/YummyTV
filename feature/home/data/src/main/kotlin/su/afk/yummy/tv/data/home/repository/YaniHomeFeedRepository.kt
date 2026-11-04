package su.afk.yummy.tv.data.home.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.common.episode.episodeNumberOrNull
import su.afk.yummy.tv.core.error.api.StringProvider
import su.afk.yummy.tv.core.model.anime.AnimeWatchProgress
import su.afk.yummy.tv.core.preferences.api.YaniAccountSettingsStore
import su.afk.yummy.tv.core.preferences.settings.PlayerSettingsStore
import su.afk.yummy.tv.core.preferences.settings.currentLanguageCode
import su.afk.yummy.tv.core.storage.anime.AnimeStorage
import su.afk.yummy.tv.core.storage.home.HomeFeedCache
import su.afk.yummy.tv.core.storage.home.HomeFeedStorage
import su.afk.yummy.tv.core.storage.home.HomeNewReleaseEntry
import su.afk.yummy.tv.core.storage.home.HOME_FEED_CONTAINER_NEW_RELEASES
import su.afk.yummy.tv.core.storage.home.isFresh
import su.afk.yummy.tv.core.storage.offlinefirst.offlineFirstCache
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStorage
import su.afk.yummy.tv.data.home.mapper.LocalFeedContext
import su.afk.yummy.tv.data.home.mapper.localContinueWatchingItems
import su.afk.yummy.tv.data.home.mapper.withLocalOverrides
import su.afk.yummy.tv.data.home.network.YaniHomeApi
import su.afk.yummy.tv.data.home.utils.YaniNewReleaseTracker
import su.afk.yummy.tv.data.home.utils.MyNewEpisodeHistoryTracker
import su.afk.yummy.tv.data.home.utils.toHomeFeedItem
import su.afk.yummy.tv.data.home.storage.mapper.toHomeContinueWatchingItem
import su.afk.yummy.tv.data.home.storage.mapper.toHomeFeedCache
import su.afk.yummy.tv.domain.home.model.ContinueWatchingProgressMigration
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.model.HomeFeed
import su.afk.yummy.tv.domain.home.model.HomeFeedItem
import su.afk.yummy.tv.domain.home.repository.HomeFeedRepository
import su.afk.yummy.tv.data.home.storage.mapper.toHomeFeed as toStoredHomeFeed

private const val FEED_TTL_MS = 60 * 1000L
private const val FEED_CACHE_SIGNATURE_VERSION = "cw-local1"
private const val TAG = "YaniHomeFeed"

class YaniHomeFeedRepository(
    private val api: YaniHomeApi,
    private val homeFeedStore: HomeFeedStorage,
    private val stringProvider: StringProvider,
    private val settingsStore: YaniAccountSettingsStore,
    private val playerSettingsStore: PlayerSettingsStore,
    private val watchProgressStore: WatchProgressStorage,
    private val animeStorage: AnimeStorage,
    private val analyticsTracker: AnalyticsTracker,
) : HomeFeedRepository {

    /** Параллельные загрузки ленты (экран и канал ТВ) ждут друг друга, а не качают её дважды. */
    private val fetchMutex = Mutex()

    override suspend fun getHomeFeed(): HomeFeed = getHomeFeed(forceRefresh = false)

    override suspend fun getCachedHomeFeed(): HomeFeed? = withContext(Dispatchers.IO) {
        val local = readLocalFeedContext()
        homeFeedStore.getFeed(local.languageCode, feedCacheSignature())
            ?.toStoredHomeFeed(stringProvider)
            ?.withLocalOverrides(local.watchEntries, local.hiddenIds)
    }

    override suspend fun refreshHomeFeed(): HomeFeed = getHomeFeed(forceRefresh = true)

    override suspend fun trackMyNewEpisodes(
        recentlyAired: List<HomeFeedItem>,
        libraryAnimeIds: Set<Int>,
        nowSeconds: Long,
    ): List<HomeFeedItem> = withContext(Dispatchers.IO) {
        val history = MyNewEpisodeHistoryTracker().merge(
            recentlyAired = recentlyAired,
            history = homeFeedStore.getMyNewEpisodeHistory(),
            libraryAnimeIds = libraryAnimeIds,
            nowSeconds = nowSeconds,
        )
        homeFeedStore.saveMyNewEpisodeHistory(history)
        history.map { it.toHomeFeedItem() }
    }

    override suspend fun removeCachedContinueWatching(animeId: Int) {
        withContext(Dispatchers.IO) {
            watchProgressStore.suppressContinueWatchingDisplay(animeId)
            homeFeedStore.deleteContinueWatchingByAnimeId(animeId)
        }
    }

    override suspend fun getContinueWatchingVideoIds(animeId: Int): List<Int> =
        withContext(Dispatchers.IO) {
            watchProgressStore.continueWatching()
                .filter { it.animeId == animeId }
                .map { it.videoId }
                .filter { it > 0 }
                .distinct()
        }

    override suspend fun migrateContinueWatchingProgress(
        migration: ContinueWatchingProgressMigration,
    ) = withContext(Dispatchers.IO) {
        watchProgressStore.save(
            animeId = migration.animeId,
            episode = migration.episode,
            videoId = migration.videoId,
            episodeUrl = migration.episodeUrl,
            positionMs = migration.positionMs,
            durationMs = migration.durationMs,
            animeTitle = migration.animeTitle,
            posterUrl = migration.posterUrl,
            playerName = migration.playerName,
            dubbing = migration.dubbing,
            screenshotUrl = migration.screenshotUrl,
        )
        watchProgressStore.delete(migration.animeId, migration.previousEpisode)
    }

    override fun observeWatchedEpisodes(): Flow<Map<Int, Set<Int>>> =
        watchProgressStore.observeWatchedProgress()
            .map { entries ->
                entries
                    .groupBy { it.animeId }
                    .mapValues { (_, items) ->
                        items.mapNotNullTo(mutableSetOf()) { entry ->
                            // Спецвыпуски вроде «7.5» — не серия 7, иначе она ложно считалась бы
                            // просмотренной.
                            entry.episode.episodeNumberOrNull()
                                ?.takeIf { it % 1.0 == 0.0 }
                                ?.toInt()
                        }
                    }
            }
            .distinctUntilChanged()

    override fun observeContinueWatching(): Flow<List<HomeContinueWatchingItem>> =
        watchProgressStore.observeContinueWatching()
            .map(::localContinueWatchingItems)
            .distinctUntilChanged()

    override fun observeWatchHistory(): Flow<List<HomeContinueWatchingItem>> =
        watchProgressStore.observeAll()
            .map { entries -> entries.map { it.toHomeContinueWatchingItem() } }
            .distinctUntilChanged()

    override fun observeServerWatchProgress(): Flow<List<AnimeWatchProgress>> =
        flow { emitAll(animeStorage.observeServerWatchProgress(settingsStore.currentLanguageCode())) }
            .distinctUntilChanged()

    override fun observeContinueWatchingSuppressions(): Flow<Map<Int, Long>> =
        watchProgressStore.observeContinueWatchingSuppressionTimestamps()
            .distinctUntilChanged()

    override fun observeSuggestNextEpisodeOnWatched(): Flow<Boolean> =
        playerSettingsStore.suggestNextEpisodeOnWatched.distinctUntilChanged()

    private suspend fun getHomeFeed(forceRefresh: Boolean): HomeFeed = withContext(Dispatchers.IO) {
        // Единый снимок на весь вызов: используется во всех трёх ветках (свежий кэш, сеть,
        // fallback при ошибке), чтобы не пересчитывать его отдельно для сетевой ветки.
        val local = readLocalFeedContext()
        val watchSignature = feedCacheSignature()
        val requestedAtMs = System.currentTimeMillis()
        offlineFirstCache(
            forceRefresh = forceRefresh,
            read = { homeFeedStore.getFeed(local.languageCode, watchSignature) },
            isFresh = { it.isFresh(FEED_TTL_MS) },
            toDomain = { it.toStoredHomeFeed(stringProvider) },
            fetchAndSave = {
                fetchMutex.withLock {
                    // Лента, сохранённая другим вызовом уже после начала этого, подходит и для него.
                    homeFeedStore.getFeed(local.languageCode, watchSignature)
                        ?.takeIf { it.cachedAt >= requestedAtMs }
                        ?: fetchHomeFeed(local.languageCode, watchSignature)
                }
            },
            transform = { it.withLocalOverrides(local.watchEntries, local.hiddenIds) },
        )
    }

    private suspend fun fetchHomeFeed(
        languageCode: String,
        watchSignature: String,
    ): HomeFeedCache {
        analyticsTracker.log(TAG) { "Fetch feed language=$languageCode watchSignature=$watchSignature" }
        val previousCache = homeFeedStore.getFeed(languageCode, watchSignature)
        val dto = api.getFeed()
        analyticsTracker.log(TAG) { "Feed dto ${dto.summaryForLog()}" }
        val now = System.currentTimeMillis()
        val storedHistory = homeFeedStore.getNewReleaseHistory()
        // Upgrade path: the previous app cached one card per anime but did not persist its
        // episode number. Keep those cards as zero-episode baselines until the API sees them
        // again, instead of dropping titles that temporarily disappeared from /feed.
        val historyForMerge = storedHistory.ifEmpty {
            previousCache.toLegacyReleaseHistory(now)
        }
        val releaseHistory = YaniNewReleaseTracker().merge(
            videos = dto.response.newVideos,
            history = historyForMerge,
            now = now,
        )
        homeFeedStore.saveNewReleaseHistory(releaseHistory)
        val mappedCache = dto.toHomeFeedCache(
            language = languageCode,
            watchSignature = watchSignature,
            cachedAt = now,
            newReleaseHistory = releaseHistory,
        )
        // Keep upgrade-era cards whose episode number was not present in the old cache. They
        // remain in their original positions until a valid feed record establishes a baseline.
        val legacyCardsByAnimeId = previousCache?.items
            ?.filter { it.container == HOME_FEED_CONTAINER_NEW_RELEASES }
            ?.associateBy { it.itemId }
            .orEmpty()
        val orderedReleaseCards = releaseHistory.sortedBy { it.position }.mapNotNull { entry ->
            if (entry.episodeNumber > 0) {
                mappedCache.items.firstOrNull {
                    it.container == HOME_FEED_CONTAINER_NEW_RELEASES && it.itemId == entry.animeId
                }
            } else {
                legacyCardsByAnimeId[entry.animeId]
            }
        }.mapIndexed { index, item -> item.copy(position = index) }
        val cache = mappedCache.copy(
            items = mappedCache.items.filterNot {
                it.container == HOME_FEED_CONTAINER_NEW_RELEASES
            } + orderedReleaseCards,
        )
        homeFeedStore.saveFeed(cache)
        analyticsTracker.log(TAG) {
            val feed = cache.toStoredHomeFeed(stringProvider)
            "Feed mapped ${feed.summaryForLog()} " +
                "continueSamples=${feed.continueWatchingItems.summaryForLog()}"
        }
        return cache
    }

    /** Язык, локальный прогресс (Room) и скрытые рекомендации независимы — читаются параллельно. */
    private suspend fun readLocalFeedContext(): LocalFeedContext = coroutineScope {
        val languageCode = async { settingsStore.currentLanguageCode() }
        val watchEntries = async { watchProgressStore.continueWatching() }
        val hiddenIds = async { settingsStore.hiddenRecommendationIds.first() }
        LocalFeedContext(
            languageCode = languageCode.await(),
            watchEntries = watchEntries.await(),
            hiddenIds = hiddenIds.await(),
        )
    }

    private fun feedCacheSignature(): String = FEED_CACHE_SIGNATURE_VERSION
}


/**
 * The previous home cache already deduplicated /feed videos by anime, but didn't persist ep_title.
 * Preserve those cards as a baseline (episode 0) until the first valid feed item is seen.
 */
private fun HomeFeedCache?.toLegacyReleaseHistory(now: Long): List<HomeNewReleaseEntry> {
    val baselineTimestamp = this?.cachedAt?.takeIf { it > 0L } ?: now
    return this?.items
        .orEmpty()
        .asSequence()
        .filter { it.container == HOME_FEED_CONTAINER_NEW_RELEASES }
        .sortedBy { it.position }
        .distinctBy { it.itemId }
        .mapIndexed { index, item ->
            HomeNewReleaseEntry(
                animeId = item.itemId,
                title = item.title,
                animeUrl = "",
                description = item.description,
                episodeNumber = 0,
                episodeTitle = "",
                videoId = null,
                dubTitle = item.dubbing.takeIf(String::isNotBlank),
                playerTitle = item.playerName.takeIf(String::isNotBlank),
                posterSmallUrl = item.posterSmallUrl,
                posterMediumUrl = item.posterMediumUrl,
                posterBigUrl = item.posterBigUrl,
                posterFullsizeUrl = item.posterFullsizeUrl,
                posterMegaUrl = item.posterMegaUrl,
                firstSeenAt = baselineTimestamp,
                lastDetectedAt = baselineTimestamp,
                lastUpdatedAt = 0L,
                position = index,
            )
        }
        .toList()
}


