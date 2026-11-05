package su.afk.yummy.tv.data.home.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.storage.home.HomeMyNewEpisodeEntry
import su.afk.yummy.tv.domain.home.model.HomeFeedItem
import su.afk.yummy.tv.domain.home.model.HomeFeedItemAction

class MyNewEpisodeHistoryTrackerTest {

    private val tracker = MyNewEpisodeHistoryTracker()

    @Test
    fun `initial snapshot establishes baseline without marking all titles updated`() {
        val result = tracker.merge(
            recentlyAired = listOf(item(1, 10), item(2, 4)),
            history = emptyList(),
            libraryAnimeIds = setOf(1, 2),
            nowSeconds = 100,
        )

        assertEquals(listOf(1, 2), result.map { it.animeId })
        assertEquals(listOf(0L, 0L), result.map { it.lastUpdatedAt })
        assertEquals(listOf(10, 4), result.map { it.episodeNumber })
    }

    @Test
    fun `higher episode moves anime to the top exactly once`() {
        val initial = tracker.merge(
            recentlyAired = listOf(item(1, 10), item(2, 7)),
            history = emptyList(),
            libraryAnimeIds = setOf(1, 2),
            nowSeconds = 100,
        )
        val updated = tracker.merge(
            recentlyAired = listOf(item(1, 11), item(2, 7)),
            history = initial,
            libraryAnimeIds = setOf(1, 2),
            nowSeconds = 200,
        )

        assertEquals(listOf(1, 2), updated.map { it.animeId })
        assertEquals(11, updated.first().episodeNumber)
        assertEquals(200L, updated.first().lastUpdatedAt)
        assertEquals(0L, updated.last().lastUpdatedAt)
    }

    @Test
    fun `reposting known or older episode does not change position or timestamp`() {
        val initial = tracker.merge(
            recentlyAired = listOf(item(1, 10), item(2, 7)),
            history = emptyList(),
            libraryAnimeIds = setOf(1, 2),
            nowSeconds = 100,
        )
        val updated = tracker.merge(
            recentlyAired = listOf(item(2, 7), item(1, 9)),
            history = initial,
            libraryAnimeIds = setOf(1, 2),
            nowSeconds = 300,
        )

        assertEquals(initial, updated)
    }

    @Test
    fun `only newest valid episode is kept for each anime`() {
        val result = tracker.merge(
            recentlyAired = listOf(item(1, 8), item(1, 10), item(1, 9)),
            history = emptyList(),
            libraryAnimeIds = setOf(1),
            nowSeconds = 100,
        )

        assertEquals(1, result.size)
        assertEquals(10, result.single().episodeNumber)
    }

    @Test
    fun `anime absent from schedule remains in persistent history`() {
        val initial = tracker.merge(
            recentlyAired = listOf(item(1, 10), item(2, 7)),
            history = emptyList(),
            libraryAnimeIds = setOf(1, 2),
            nowSeconds = 100,
        )
        val result = tracker.merge(
            recentlyAired = emptyList(),
            history = initial,
            libraryAnimeIds = setOf(1, 2),
            nowSeconds = 200,
        )

        assertEquals(initial, result)
    }

    @Test
    fun `same title with different anime ids stays separate`() {
        val result = tracker.merge(
            recentlyAired = listOf(item(1, 10, title = "Same"), item(2, 4, title = "Same")),
            history = emptyList(),
            libraryAnimeIds = setOf(1, 2),
            nowSeconds = 100,
        )

        assertEquals(setOf(1, 2), result.map { it.animeId }.toSet())
        assertEquals(2, result.size)
    }

    @Test
    fun `invalid or missing episode number is ignored`() {
        val result = tracker.merge(
            recentlyAired = listOf(item(1, null), item(2, 0)),
            history = emptyList(),
            libraryAnimeIds = setOf(1, 2),
            nowSeconds = 100,
        )

        assertEquals(emptyList<HomeMyNewEpisodeEntry>(), result)
    }

    @Test
    fun `anime outside selected library is not added`() {
        val result = tracker.merge(
            recentlyAired = listOf(item(1, 10)),
            history = emptyList(),
            libraryAnimeIds = emptySet(),
            nowSeconds = 100,
        )

        assertEquals(emptyList<HomeMyNewEpisodeEntry>(), result)
    }

    private fun item(
        id: Int,
        episode: Int?,
        title: String = "Title $id",
    ) = HomeFeedItem(
        id = id,
        title = title,
        description = "",
        poster = null,
        rating = null,
        year = null,
        action = HomeFeedItemAction.OpenSeries(id),
        episodeNumber = episode,
        airedAtSeconds = 1_800_000_000L + (episode ?: 0),
    )
}
