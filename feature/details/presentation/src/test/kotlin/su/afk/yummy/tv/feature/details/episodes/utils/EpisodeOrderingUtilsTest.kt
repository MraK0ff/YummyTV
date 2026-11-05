package su.afk.yummy.tv.feature.details.episodes.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.model.anime.AnimeVideo

class EpisodeOrderingUtilsTest {

    @Test
    fun `previous groups follow numeric episode order across seasons`() {
        val videos = listOf(
            video("2.2"),
            video("1.2"),
            video("2.1"),
            video("1.1"),
            video("2.3"),
        )

        val groups = buildEpisodeGroups(videos)
        val previous = previousEpisodeGroups(groups, "2.1", listOf(video("2.1")))

        assertEquals(listOf("1.1", "1.2", "2.1"), previous.map { it.episode })
    }

    @Test
    fun `special and unknown episodes keep the existing trailing stable order`() {
        val videos = listOf(
            video("OVA"),
            video("2"),
            video("Special"),
            video("1"),
            video("Extra"),
        )

        val groups = buildEpisodeGroups(videos)
        val previous = previousEpisodeGroups(groups, "Special", listOf(video("Special")))

        assertEquals(listOf("1", "2", "ova", "special"), previous.map { it.episode })
    }

    @Test
    fun `selected episode is included when it is not in loaded groups`() {
        val groups = buildEpisodeGroups(listOf(video("1"), video("2")))

        val previous = previousEpisodeGroups(groups, "3", listOf(video("3")))

        assertEquals(1, previous.size)
        assertEquals("3", previous.single().episode)
    }

    private fun video(episode: String) = AnimeVideo(
        id = episode.hashCode(),
        episode = episode,
        dubbing = "Test",
        player = "Kodik",
        playerId = null,
        iframeUrl = "https://example.invalid/$episode",
        durationSeconds = 1_440,
    )
}
