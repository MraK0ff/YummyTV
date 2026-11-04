package su.afk.yummy.tv.source.anistar.data.torrent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AniStarTorrentParserTest {
    @Test
    fun `parses multiple torrents and optional stats`() {
        val html = """
            <div class="torrent">
              <div class="torrent-inner">
                <a href="/torrent/1"><span class="info_d1">Anime &amp; Movie (1080p)</span></a>
                <span>Размер: <b>1.4 GB</b></span>
                <li class="li_swing">12</li>
                <li class="li_distribute">34</li>
                <li class="li_download">56</li>
              </div>
            </div>
            <div class="torrent">
              <a href="https://anistar.example/torrent/2"><span class="info_d1">Second</span></a>
            </div>
        """.trimIndent()

        val torrents = AniStarTorrentParser.parseList(html, "https://anistar.example/list")

        assertEquals(2, torrents.size)
        assertEquals("Anime & Movie (1080p)", torrents[0].title)
        assertEquals("https://anistar.example/torrent/1", torrents[0].url)
        assertEquals("1.4 GB", torrents[0].size)
        assertEquals(34, torrents[0].seeders)
        assertEquals(12, torrents[0].leechers)
        assertEquals(56, torrents[0].downloads)
        assertNull(torrents[1].size)
        assertNull(torrents[1].seeders)
    }

    @Test
    fun `returns empty list for changed or empty html`() {
        assertTrue(AniStarTorrentParser.parseList("", "https://anistar.example").isEmpty())
        assertTrue(AniStarTorrentParser.parseList("<html>layout changed</html>", "https://anistar.example").isEmpty())
    }

    @Test
    fun `resolves magnet and torrent download from detail page`() {
        val page = AniStarTorrentParser.parseTorrentPage(
            """<title>Anime &amp; Special</title><a href="magnet:?xt=urn:btih:0123456789012345678901234567890123456789">Magnet</a><a href="/download/a.torrent">Download</a>""",
            "https://anistar.example/torrent/4",
        )

        assertEquals("Anime & Special", page.title)
        assertEquals("magnet:?xt=urn:btih:0123456789012345678901234567890123456789", page.magnetUri)
        assertEquals("https://anistar.example/download/a.torrent", page.torrentFileUrl)
    }

    @Test
    fun `ignores unsafe torrent and magnet links`() {
        val page = AniStarTorrentParser.parseTorrentPage(
            """<a href="javascript:alert(1)">Torrent</a><a href="file:///etc/passwd.torrent">File</a><a href="magnet:?dn=missing-info-hash">Bad magnet</a>""",
            "https://anistar.example/torrent/5",
        )

        assertNull(page.magnetUri)
        assertNull(page.torrentFileUrl)
    }

    @Test
    fun `ignores non-http catalog destinations`() {
        val html = """<div class="torrent"><a href="javascript:alert(1)"><span class="info_d1">Unsafe</span></a></div>"""

        assertTrue(AniStarTorrentParser.parseList(html, "https://anistar.example/list").isEmpty())
    }
}
