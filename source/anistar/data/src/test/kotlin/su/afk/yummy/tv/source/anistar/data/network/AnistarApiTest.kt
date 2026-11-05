package su.afk.yummy.tv.source.anistar.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import java.io.IOException

class AnistarApiTest : BaseUnitTest() {

    private val requests = mutableListOf<HttpRequestData>()

    private fun api(
        status: HttpStatusCode = HttpStatusCode.OK,
        body: String = "<html></html>",
    ): AnistarApi {
        val engine = MockEngine { request ->
            requests += request
            if (status.value in 200..299) {
                respond(body, status, headersOf(HttpHeaders.ContentType, "text/html"))
            } else {
                respondError(status)
            }
        }
        return AnistarApi(HttpClient(engine))
    }

    @Test
    fun `search sends query to AniStar and returns response mirror`() = runTest {
        val page = api(body = "<div>Frieren</div>").search("Frieren")

        assertEquals(HttpMethod.Get, requests.single().method)
        assertEquals("search", requests.single().url.parameters["do"])
        assertEquals("search", requests.single().url.parameters["subaction"])
        assertEquals("Frieren", requests.single().url.parameters["story"])
        assertEquals("https://v31.astar.bz", page.mirror)
        assertEquals("<div>Frieren</div>", page.html)
    }

    @Test
    fun `player page is requested by news id on the given mirror`() = runTest {
        val html = api(body = "<html>player</html>")
            .getPlayerPage("https://v31.astar.bz", 2658)

        assertEquals(
            "https://v31.astar.bz/test/player2/videoas_p2p_new.php?id=2658",
            requests.single().url.toString(),
        )
        assertEquals("<html>player</html>", html)
    }

    @Test
    fun `search server errors are io exceptions`() = runTest {
        assertThrows(IOException::class.java) {
            kotlinx.coroutines.runBlocking { api(HttpStatusCode.BadGateway).search("Frieren") }
        }
    }

    @Test
    fun `player page errors are io exceptions`() = runTest {
        assertThrows(IOException::class.java) {
            kotlinx.coroutines.runBlocking {
                api(HttpStatusCode.Forbidden).getPlayerPage("https://v31.astar.bz", 1)
            }
        }
    }
}
