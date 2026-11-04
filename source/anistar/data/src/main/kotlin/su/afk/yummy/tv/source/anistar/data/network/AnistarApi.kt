package su.afk.yummy.tv.source.anistar.data.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import su.afk.yummy.tv.core.network.di.UnauthenticatedJsonClient
import java.io.IOException
import javax.inject.Inject

/**
 * Клиент AniStar. Поиск выполняется напрямую на сайте, а не через серверный индекс YummyTV:
 * так новые тайтлы и серии становятся видны сразу после публикации на AniStar.
 */
internal class AnistarApi @Inject constructor(
    @param:UnauthenticatedJsonClient private val client: HttpClient,
) {
    suspend fun search(query: String): AnistarSearchPage {
        val response = client.get("$SITE_URL/index.php") {
            url.parameters.append("do", "search")
            url.parameters.append("subaction", "search")
            url.parameters.append("search_start", "1")
            url.parameters.append("full_search", "0")
            url.parameters.append("result_from", "1")
            url.parameters.append("story", query)
        }
        if (!response.status.isSuccess()) throw IOException("AniStar search: HTTP ${response.status.value}")
        val requestUrl = response.request.url
        val mirror = "${requestUrl.protocol.name}://${requestUrl.host}"
        return AnistarSearchPage(response.bodyAsText(), mirror)
    }

    /** Плеер-страница содержит актуальный JS-плейлист; её читаем при каждом запросе источника. */
    suspend fun getPlayerPage(mirror: String, newsId: Long): String {
        val response = client.get("$mirror/test/player2/videoas_p2p_new.php") {
            url.parameters.append("id", newsId.toString())
        }
        if (!response.status.isSuccess()) throw IOException("AniStar player: HTTP ${response.status.value}")
        return response.bodyAsText()
    }

    private companion object {
        // Домен совпадает с актуальным доменом из конфигурации AniStar; Ktor следует HTTP redirect.
        const val SITE_URL = "https://v31.astar.bz"
    }
}

internal data class AnistarSearchPage(
    val html: String,
    val mirror: String,
)
