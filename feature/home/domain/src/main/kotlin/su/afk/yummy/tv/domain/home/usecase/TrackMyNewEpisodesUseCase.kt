package su.afk.yummy.tv.domain.home.usecase

import su.afk.yummy.tv.domain.home.model.HomeFeedItem
import su.afk.yummy.tv.domain.home.repository.HomeFeedRepository
import javax.inject.Inject

/** Persists and orders the latest known aired episode for each selected library anime. */
class TrackMyNewEpisodesUseCase @Inject constructor(
    private val homeFeedRepository: HomeFeedRepository,
) {
    suspend operator fun invoke(
        recentlyAired: List<HomeFeedItem>,
        libraryAnimeIds: Set<Int>,
        nowSeconds: Long,
    ): List<HomeFeedItem> = homeFeedRepository.trackMyNewEpisodes(
        recentlyAired = recentlyAired,
        libraryAnimeIds = libraryAnimeIds,
        nowSeconds = nowSeconds,
    )
}
