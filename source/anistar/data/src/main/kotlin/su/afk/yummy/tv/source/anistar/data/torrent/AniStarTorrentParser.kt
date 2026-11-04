package su.afk.yummy.tv.source.anistar.data.torrent

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.net.URI
import su.afk.yummy.tv.source.anistar.data.utils.findBestAnistarCandidate

/**
 * HTML-парсер каталога и страницы раздачи AniStar.
 * Jsoup учитывает вложенные элементы и HTML entities, поэтому вложенная разметка карточки
 * не обрезается так, как это происходит при разборе HTML регулярными выражениями.
 */
internal object AniStarTorrentParser {

    fun parseList(html: String, pageUrl: String): List<AniStarTorrent> {
        val document = Jsoup.parse(html, pageUrl)
        val blocks = document.select(".torrent")
        if (blocks.isNotEmpty()) {
            return blocks.mapNotNull { block ->
                val anchor = block.selectFirst("a[href]") ?: return@mapNotNull null
                val rawHref = anchor.attr("href")
                val href = anchor.absUrl("href")
                val name = block.selectFirst(".info_d1")?.text()?.trim()
                    ?.takeIf(String::isNotBlank)
                    ?: anchor.text().trim().takeIf(String::isNotBlank)
                    ?: return@mapNotNull null
                when {
                    isMagnetUri(rawHref) -> AniStarTorrent(
                        title = name,
                        url = pageUrl,
                        size = extractSize(block),
                        seeders = block.selectFirst(".li_distribute")?.text()?.toIntOrNull(),
                        leechers = block.selectFirst(".li_swing")?.text()?.toIntOrNull(),
                        downloads = block.selectFirst(".li_download")?.text()?.toIntOrNull(),
                        magnetUri = rawHref,
                    )
                    rawHref.substringBefore('?').endsWith(".torrent", ignoreCase = true) && isHttpUrl(href) ->
                        AniStarTorrent(
                            title = name,
                            url = pageUrl,
                            size = extractSize(block),
                            seeders = block.selectFirst(".li_distribute")?.text()?.toIntOrNull(),
                            leechers = block.selectFirst(".li_swing")?.text()?.toIntOrNull(),
                            downloads = block.selectFirst(".li_download")?.text()?.toIntOrNull(),
                            torrentFileUrl = href,
                        )
                    isHttpUrl(href) -> AniStarTorrent(
                        title = name,
                        url = href,
                        size = extractSize(block),
                        seeders = block.selectFirst(".li_distribute")?.text()?.toIntOrNull(),
                        leechers = block.selectFirst(".li_swing")?.text()?.toIntOrNull(),
                        downloads = block.selectFirst(".li_download")?.text()?.toIntOrNull(),
                    )
                    else -> null
                }
            }.distinctBy { "${it.url}|${it.magnetUri}|${it.torrentFileUrl}" }
        }

        // AniStar exposes its torrent list through the #torrent_all section. Some site layouts
        // render simple links there instead of the legacy .torrent card markup.
        val torrentSection = document.selectFirst("#torrent_all") ?: return emptyList()
        val links = if (torrentSection.select("a[href]").isNotEmpty()) {
            torrentSection.select("a[href]")
        } else {
            torrentSection.parent()?.select("a[href]") ?: emptyList()
        }
        return links.mapNotNull { anchor ->
            val rawHref = anchor.attr("href")
            val href = anchor.absUrl("href")
            val label = anchor.text().trim()
            if (label.isBlank()) return@mapNotNull null
            if (isMagnetUri(rawHref)) {
                AniStarTorrent(
                    title = label,
                    url = pageUrl,
                    size = null,
                    seeders = null,
                    leechers = null,
                    magnetUri = rawHref,
                )
            } else if (rawHref.substringBefore('?').endsWith(".torrent", ignoreCase = true) && isHttpUrl(href)) {
                AniStarTorrent(
                    title = label,
                    url = pageUrl,
                    size = null,
                    seeders = null,
                    leechers = null,
                    torrentFileUrl = href,
                )
            } else if (isHttpUrl(href)) {
                AniStarTorrent(
                    title = label,
                    url = href,
                    size = null,
                    seeders = null,
                    leechers = null,
                )
            } else {
                null
            }
        }.distinctBy { "${it.url}|${it.magnetUri}|${it.torrentFileUrl}" }
    }

    fun parseSearchResultPage(html: String, pageUrl: String, title: String): String? {
        val candidate = findBestAnistarCandidate(html, listOf(title), expectedYear = null) ?: return null
        val document = Jsoup.parse(html, pageUrl)
        val titlePattern = Regex("(?:^|/)" + candidate.newsId + "-")
        return document.select("a[href]").firstOrNull { anchor ->
            val path = runCatching { URI(anchor.absUrl("href")).path }.getOrNull().orEmpty()
            Regex("""(?:^|/)\d+-[^/]+\.html$""", RegexOption.IGNORE_CASE).containsMatchIn(path) &&
                titlePattern.containsMatchIn(path)
        }?.absUrl("href")?.takeIf(::isHttpUrl)
    }

    fun parseTorrentPage(html: String, pageUrl: String): AniStarTorrentPage {
        val document = Jsoup.parse(html, pageUrl)
        val magnetUri = document.select("a[href]").firstOrNull { anchor ->
            isMagnetUri(anchor.attr("href"))
        }?.attr("href")
        val torrentUrl = document.select("a[href]").firstOrNull { anchor ->
            anchor.attr("href").substringBefore('?').endsWith(".torrent", ignoreCase = true) &&
                isHttpUrl(anchor.absUrl("href"))
        }?.absUrl("href")
        val pageTitle = document.title().trim().takeIf(String::isNotBlank)
        return AniStarTorrentPage(
            title = pageTitle,
            magnetUri = magnetUri,
            torrentFileUrl = torrentUrl,
        )
    }

    private fun isHttpUrl(value: String): Boolean {
        val uri = runCatching { URI(value) }.getOrNull() ?: return false
        return (uri.scheme.equals("http", ignoreCase = true) ||
            uri.scheme.equals("https", ignoreCase = true)) &&
            !uri.host.isNullOrBlank() && uri.userInfo == null &&
            uri.port in -1..65535
    }

    private fun isMagnetUri(value: String): Boolean {
        if (value.any(Char::isISOControl)) return false
        val uri = runCatching { URI(value) }.getOrNull() ?: return false
        return uri.scheme.equals("magnet", ignoreCase = true) &&
            value.substringAfter('?', "").split('&').any { it.startsWith("xt=urn:btih:", ignoreCase = true) }
    }

    private fun extractSize(block: Element): String? {
        val ownText = block.getAllElements()
            .asSequence()
            .firstOrNull { it.ownText().contains("Размер", ignoreCase = true) }
            ?.text()
            .orEmpty()
        val match = Regex("""Размер\s*:\s*(.+)""", RegexOption.IGNORE_CASE)
            .find(ownText)?.groupValues?.getOrNull(1)?.trim()
        return match?.takeIf(String::isNotBlank)
    }
}
