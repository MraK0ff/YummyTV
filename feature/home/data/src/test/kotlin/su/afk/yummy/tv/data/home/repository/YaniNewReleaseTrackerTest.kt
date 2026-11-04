package su.afk.yummy.tv.data.home.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.data.home.dto.YaniVideoDto

class YaniNewReleaseTrackerTest {

    private val tracker = YaniNewReleaseTracker()

    @Test
    fun `alternate dubs of one episode produce one anime card with stable selection`() {
        val result = tracker.merge(
            videos = listOf(video(1, "10", dub = "Zulu", videoId = 2), video(1, "10", dub = "Alpha", videoId = 1)),
            history = emptyList(),
            now = 100L,
        )

        assertEquals(1, result.size)
        assertEquals(10, result.single().episodeNumber)
        assertEquals("Alpha", result.single().dubTitle)
    }

    @Test
    fun `initial snapshot is baseline and repost of episode ten does not move card`() {
        val initial = tracker.merge(listOf(video(1, "10")), emptyList(), now = 100L)
        val repost = tracker.merge(
            listOf(video(1, "1"), video(1, "10", dub = "Another")),
            initial,
            now = 200L,
        )

        assertEquals(initial.single().position, repost.single().position)
        assertEquals(0L, repost.single().lastUpdatedAt)
        assertEquals(10, repost.single().episodeNumber)
        assertEquals(200L, repost.single().lastDetectedAt)
    }

    @Test
    fun `episode eleven moves anime above previous cards`() {
        val initial = tracker.merge(listOf(video(1, "10"), video(2, "5")), emptyList(), now = 100L)
        val updated = tracker.merge(listOf(video(1, "11")), initial, now = 200L)

        assertEquals(1, updated.first().animeId)
        assertEquals(11, updated.first().episodeNumber)
        assertEquals(200L, updated.first().lastUpdatedAt)
        assertEquals(2, updated.last().animeId)
    }

    @Test
    fun `feed containing episodes one through ten displays only ten`() {
        val result = tracker.merge((1..10).map { video(1, it.toString()) }, emptyList(), now = 100L)

        assertEquals(1, result.size)
        assertEquals(10, result.single().episodeNumber)
    }

    @Test
    fun `anime missing from api remains in history`() {
        val initial = tracker.merge(listOf(video(1, "10"), video(2, "4")), emptyList(), now = 100L)
        val refreshed = tracker.merge(listOf(video(1, "11")), initial, now = 200L)

        assertEquals(setOf(1, 2), refreshed.map { it.animeId }.toSet())
    }

    @Test
    fun `history passed back after recreation retains episode and ordering`() {
        val beforeRestart = tracker.merge(listOf(video(1, "10"), video(2, "4")), emptyList(), now = 100L)
        // The Room-backed list is the input after a new repository/process instance is created.
        val afterRestart = YaniNewReleaseTracker().merge(listOf(video(1, "10", dub = "Repost")), beforeRestart, now = 300L)

        assertEquals(beforeRestart.map { it.animeId }, afterRestart.map { it.animeId })
        assertEquals(10, afterRestart.first { it.animeId == 1 }.episodeNumber)
        assertEquals(0L, afterRestart.first { it.animeId == 1 }.lastUpdatedAt)
    }

    @Test
    fun `reposting an older episode does not change position`() {
        val initial = tracker.merge(listOf(video(1, "10"), video(2, "5")), emptyList(), now = 100L)
        val repost = tracker.merge(listOf(video(2, "4")), initial, now = 200L)

        assertEquals(initial.map { it.animeId }, repost.map { it.animeId })
        assertEquals(initial.map { it.position }, repost.map { it.position })
    }

    @Test
    fun `same titles with different anime ids remain separate`() {
        val result = tracker.merge(
            listOf(video(1, "10", title = "Same title"), video(2, "3", title = "Same title")),
            emptyList(),
            now = 100L,
        )

        assertEquals(2, result.size)
        assertNotEquals(result[0].animeId, result[1].animeId)
    }

    @Test
    fun `invalid episode titles are ignored safely`() {
        val result = tracker.merge(
            listOf(video(1, "OVA"), video(2, "SP"), video(3, "Movie"), video(4, "7.5"), video(5, "oops")),
            emptyList(),
            now = 100L,
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `empty api response never clears stored history`() {
        val initial = tracker.merge(listOf(video(1, "10")), emptyList(), now = 100L)

        assertEquals(initial, tracker.merge(emptyList(), initial, now = 200L))
    }

    @Test
    fun `duplicate records in one response do not create duplicate cards`() {
        val result = tracker.merge(
            listOf(video(1, "10"), video(1, "10"), video(1, "9")),
            emptyList(),
            now = 100L,
        )

        assertEquals(1, result.size)
        assertEquals(10, result.single().episodeNumber)
    }

    @Test
    fun `normalizes leading zeros and zero decimal suffix`() {
        assertEquals(10, " 010 ".toRegularEpisodeNumber())
        assertEquals(10, "10.0".toRegularEpisodeNumber())
        assertEquals(10, "10,00 серия".toRegularEpisodeNumber())
        assertFalse("10.5".toRegularEpisodeNumber() == 10)
    }

    private fun video(
        animeId: Int,
        episode: String,
        dub: String = "Dub",
        videoId: Int = 1,
        title: String = "Anime $animeId",
    ) = YaniVideoDto(
        videoId = videoId,
        animeId = animeId,
        animeUrl = "/anime/$animeId",
        title = title,
        description = "Description",
        episodeTitle = episode,
        dubTitle = dub,
        playerTitle = "Player",
    )
}
