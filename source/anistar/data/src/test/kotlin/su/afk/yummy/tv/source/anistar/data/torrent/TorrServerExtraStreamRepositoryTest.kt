package su.afk.yummy.tv.source.anistar.data.torrent

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult

class TorrServerExtraStreamRepositoryTest {

    private val client = TorrServerClient(
        HttpClient(MockEngine { error("No network request expected") }),
        object : TorrServerConfigurationStore {
            override fun load() = TorrServerConfiguration(baseUrl = "http://torr.example:8090")
            override fun save(configuration: TorrServerConfiguration) = Unit
        },
    )
    private val repository = TorrServerExtraStreamRepository(client)

    @Test
    fun `supports only TorrServer source references`() {
        assertTrue(repository.supports("source://torrserver/01234567890123456789/0"))
        assertFalse(repository.supports("source://anistar/123/1"))
    }

    @Test
    fun `resolves source reference to Media3 http stream`() = runBlocking {
        val result = repository.resolve("source://torrserver/01234567890123456789/2")

        assertTrue(result is PlayerStreamResolveResult.Stream)
        assertEquals(
            "http://torr.example:8090/play/01234567890123456789/2",
            (result as PlayerStreamResolveResult.Stream).url,
        )
    }

    @Test
    fun `rejects invalid source reference`() = runBlocking {
        val result = repository.resolve("source://torrserver/not-a-hash/-1")

        assertTrue(result is PlayerStreamResolveResult.Failed)
    }
}
