package su.afk.yummy.tv.data.home.storage.mapper

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.error.api.StringProvider
import su.afk.yummy.tv.data.home.dto.YaniAnimeDto
import su.afk.yummy.tv.data.home.dto.YaniFeedDto
import su.afk.yummy.tv.data.home.dto.YaniFeedResponseDto
import su.afk.yummy.tv.data.home.dto.YaniVideoDto
import su.afk.yummy.tv.data.home.utils.YaniNewReleaseTracker
import su.afk.yummy.tv.domain.home.model.HomeFeedSectionType

class HomeStorageMapperTest {

    private val stringProvider: StringProvider = mockk {
        every { get(any<Int>()) } returns "title"
    }

    @Test
    fun `announcements and new titles become their own sections`() {
        val feed = YaniFeedDto(
            YaniFeedResponseDto(
                announcements = listOf(anime(1), anime(2)),
                new = listOf(anime(3)),
                recommends = listOf(anime(4)),
            ),
        ).toHomeFeedCache(language = "ru", watchSignature = "", cachedAt = 0L)
            .toHomeFeed(stringProvider)

        assertEquals(
            listOf(
                HomeFeedSectionType.NEW_TITLES,
                HomeFeedSectionType.RECOMMENDATIONS,
                HomeFeedSectionType.ANNOUNCEMENTS,
            ),
            feed.sections.map { it.type },
        )
        assertEquals(
            listOf(1, 2),
            feed.sections.single { it.type == HomeFeedSectionType.ANNOUNCEMENTS }.items.map { it.id },
        )
        assertEquals(
            listOf(3),
            feed.sections.single { it.type == HomeFeedSectionType.NEW_TITLES }.items.map { it.id },
        )
    }

    @Test
    fun `tracked updates stay in the existing single new releases section`() {
        val videos = listOf(
            YaniVideoDto(animeId = 1, title = "Same", episodeTitle = "10", dubTitle = "Dub A"),
            YaniVideoDto(animeId = 1, title = "Same", episodeTitle = "10", dubTitle = "Dub B"),
            YaniVideoDto(animeId = 2, title = "Other", episodeTitle = "3"),
        )
        val history = YaniNewReleaseTracker().merge(videos, emptyList(), now = 100L)
        val feed = YaniFeedDto(
            YaniFeedResponseDto(newVideos = videos),
        ).toHomeFeedCache(
            language = "ru",
            watchSignature = "",
            cachedAt = 100L,
            newReleaseHistory = history,
        ).toHomeFeed(stringProvider)

        val updates = feed.sections.filter { it.type == HomeFeedSectionType.NEW_RELEASES }
        assertEquals(1, updates.size)
        assertEquals(listOf(1, 2), updates.single().items.map { it.id })
        assertEquals(10, updates.single().items.first().episodeNumber)
    }

    private fun anime(id: Int) = YaniAnimeDto(animeId = id, title = "Anime $id")
}
