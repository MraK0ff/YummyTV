package su.afk.yummy.tv.source.anistar.data.source

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.AnimeEpisodes
import su.afk.yummy.tv.core.model.anime.AnimeRating
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import su.afk.yummy.tv.source.anistar.data.network.AnistarApi
import su.afk.yummy.tv.source.anistar.data.network.AnistarSearchPage
import su.afk.yummy.tv.source.anistar.domain.model.AnistarStreams
import su.afk.yummy.tv.source.anistar.domain.repository.AnistarRepository
import su.afk.yummy.tv.source.anistar.domain.usecase.GetAnistarEpisodesUseCase
import su.afk.yummy.tv.source.anistar.domain.usecase.ResolveAnistarStreamsUseCase
import java.io.IOException

class AnistarExtraSourceTest : BaseUnitTest() {

    private val repository: AnistarRepository = mockk()
    private val api: AnistarApi = mockk()
    private val messages = object : AnistarMessages {
        override val notFound = "nf-text"
    }

    private fun details(malId: Int? = 52991) = AnimeDetails(
        id = 1, animeUrl = "u", title = "Фрирен", description = "", poster = null,
        rating = AnimeRating(null, null, null, null, null), genres = emptyList(), year = 2023, ageRating = null,
        views = null, status = null, type = "tv", episodes = AnimeEpisodes(28, 12, null, null), otherTitles = listOf("Frieren"),
        creators = emptyList(), studios = emptyList(), viewingOrder = emptyList(), screenshots = emptyList(), malId = malId,
    )

    private val searchPage = AnistarSearchPage(
        html = """<div id="dle-content"><div class="shortstory"><a href="/2658-frieren.html">Фрирен / Frieren</a><span>Год выпуска: 2023</span></div></div>""",
        mirror = "https://v31.astar.bz",
    )

    private val playerPage = """
        title: "Серия 2", media_id: 2,
        files: [{title: "360", file: "https://cdn/360.m3u8"}],
        title: "Серия 1", media_id: 1,
        files: [{title: "360", file: "https://cdn/360.m3u8"}],
    """.trimIndent()

    private fun videoRepository() = AnistarExtraVideoRepository(api, GetAnistarEpisodesUseCase())

    private fun streamRepository() = AnistarExtraStreamRepository(ResolveAnistarStreamsUseCase(repository), messages)

    @Test
    fun `videos are found directly on AniStar and do not require MAL id`() = runTest {
        coEvery { api.search("Фрирен") } returns searchPage
        coEvery { api.getPlayerPage("https://v31.astar.bz", 2658L) } returns playerPage

        val videos = videoRepository().videos(details(malId = null))

        assertEquals(listOf("1", "2"), videos.map { it.episode })
        assertEquals("source://anistar/2658/1", videos[0].iframeUrl)
        assertTrue(videos.all { it.player == "AniStar" && it.dubbing == "AniStar" && it.id < 0 })
        assertEquals(videos.size, videos.map { it.id }.toSet().size)
    }

    @Test
    fun `direct search errors are propagated instead of silently hiding source`() = runTest {
        coEvery { api.search("Фрирен") } throws IOException("AniStar is unavailable")

        assertThrows(IOException::class.java) {
            kotlinx.coroutines.runBlocking { videoRepository().videos(details()) }
        }
    }

    @Test
    fun `stream carries ascending qualities the best one and referer`() = runTest {
        coEvery { repository.getStreams(any()) } returns AnistarStreams(
            hlsByQuality = mapOf(720 to "https://cdn/720.m3u8", 360 to "https://cdn/360.m3u8"),
            referer = "https://v31.astar.bz/",
        )

        val result = streamRepository().resolve("source://anistar/2658/1") as PlayerStreamResolveResult.Stream

        assertEquals("https://cdn/720.m3u8", result.url)
        assertEquals(listOf("360p", "720p"), result.qualities?.keys?.toList())
        assertEquals(mapOf("Referer" to "https://v31.astar.bz/"), result.headers)
    }

    @Test
    fun `empty or invalid quality list fails without crashing`() = runTest {
        coEvery { repository.getStreams(any()) } returns AnistarStreams(
            hlsByQuality = mapOf(0 to "", -1 to "https://cdn/invalid.m3u8"),
            referer = "https://v31.astar.bz/",
        )

        val result = streamRepository().resolve("source://anistar/2658/1")

        assertTrue(result is PlayerStreamResolveResult.Failed)
    }

    @Test
    fun `missing episode is unavailable network error and bad url fail`() = runTest {
        coEvery { repository.getStreams(any()) } returns null
        val unavailable = streamRepository().resolve("source://anistar/2658/9")
        assertEquals("nf-text", (unavailable as PlayerStreamResolveResult.Unavailable).message)

        coEvery { repository.getStreams(any()) } throws IOException("down")
        assertTrue(streamRepository().resolve("source://anistar/2658/1") is PlayerStreamResolveResult.Failed)
        assertTrue(streamRepository().resolve("source://anistar/x") is PlayerStreamResolveResult.Failed)
    }

    @Test
    fun `only anistar urls are supported`() {
        assertTrue(streamRepository().supports("source://anistar/2658/1"))
        assertEquals(false, streamRepository().supports("source://aniboom/0BzM07Pq71J/1/16"))
    }
}
