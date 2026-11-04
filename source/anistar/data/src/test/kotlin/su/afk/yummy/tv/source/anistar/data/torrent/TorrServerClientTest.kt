package su.afk.yummy.tv.source.anistar.data.torrent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest

class TorrServerClientTest : BaseUnitTest() {

    private val configurationStore = TestTorrServerConfigurationStore()
    private val client = TorrServerClient(
        io.ktor.client.HttpClient(io.ktor.client.engine.mock.MockEngine { request ->
            error("Unexpected HTTP request: ${request.url}")
        }),
        configurationStore,
    )

    @Test
    fun `normalizes configured base url`() {
        client.configure(TorrServerConfiguration(baseUrl = "https://torr.example:8090/"))

        assertEquals(
            "https://torr.example:8090/play/0123456789abcdef0123456789abcdef/2",
            client.playbackUrl("0123456789abcdef0123456789abcdef", 2),
        )
        assertEquals("https://torr.example:8090", configurationStore.saved?.baseUrl)
    }

    @Test
    fun `rejects unsupported base url schemes`() {
        assertThrows(IllegalArgumentException::class.java) {
            client.configure(TorrServerConfiguration(baseUrl = "ftp://torr.example"))
        }
    }

    @Test
    fun `rejects line breaks in credentials`() {
        assertThrows(IllegalArgumentException::class.java) {
            client.configure(TorrServerConfiguration(username = "user\nInjected: value"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            client.configure(TorrServerConfiguration(password = "secret\rInjected: value"))
        }
    }

    @Test
    fun `rejects invalid playback hashes and indexes`() {
        assertThrows(IllegalArgumentException::class.java) {
            client.playbackUrl("not-a-hash")
        }
        assertThrows(IllegalArgumentException::class.java) {
            client.playbackUrl("0123456789abcdef0123456789abcdef", -1)
        }
    }

    @Test
    fun `rejects unsupported torrent source before request`() {
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking {
                client.add("file:///storage/download.torrent")
            }
        }
    }

    @Test
    fun `rejects empty and oversized torrent uploads before request`() {
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking {
                client.uploadTorrentFile(byteArrayOf())
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking {
                client.uploadTorrentFile(ByteArray(8 * 1024 * 1024 + 1))
            }
        }
    }
}


private class TestTorrServerConfigurationStore : TorrServerConfigurationStore {
    var savedBaseUrl: String? = null

    override fun load(): TorrServerConfiguration =
        TorrServerConfiguration(baseUrl = savedBaseUrl ?: "http://127.0.0.1:8090")

    override fun saveBaseUrl(baseUrl: String) {
        savedBaseUrl = baseUrl
    }
}
