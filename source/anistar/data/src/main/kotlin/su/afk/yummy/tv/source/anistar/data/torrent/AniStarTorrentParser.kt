package su.afk.yummy.tv.source.anistar.data.torrent

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * HTML-парсер каталога и страницы раздачи AniStar.
 * Jsoup учитывает вложенные элементы и HTML entities, поэтому вложенная разметка карточки
 * не обрезается так, как это происходит при разборе HTML регулярными выражениями.
 */
internal object AniStarTorrentParser {

    fun parseList(html: String, pageUrl: String): List<AniStarTorrent> {
        val document = Jsoup.parse(html, pageUrl)
        return document.select(".torrent").mapNotNull { block ->
            val link = block.selectFirst("a[href]")?.absUrl("href")
                ?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            val name = block.selectFirst(".info_d1")?.text()?.trim()
                ?.takeIf(String::isNotBlank)
                ?: block.selectFirst("a[href]")?.text()?.trim()?.takeIf(String::isNotBlank)
                ?: return@mapNotNull null

            AniStarTorrent(
                title = name,
                url = link,
                size = extractSize(block),
                seeders = block.selectFirst(".li_distribute")?.text()?.toIntOrNull(),
                leechers = block.selectFirst(".li_swing")?.text()?.toIntOrNull(),
                downloads = block.selectFirst(".li_download")?.text()?.toIntOrNull(),
            )
        }
    }

    fun parseTorrentPage(html: String, pageUrl: String): AniStarTorrentPage {
        val document = Jsoup.parse(html, pageUrl)
        val magnetUri = document.selectFirst("a[href^=magnet:]")?.attr("href")
            ?.takeIf(String::isNotBlank)
        val torrentUrl = document.select("a[href]").firstOrNull { anchor ->
            anchor.attr("href").substringBefore('?').endsWith(".torrent", ignoreCase = true)
        }?.absUrl("href")?.takeIf(String::isNotBlank)
        val pageTitle = document.title().trim().takeIf(String::isNotBlank)
        return AniStarTorrentPage(
            title = pageTitle,
            magnetUri = magnetUri,
            torrentFileUrl = torrentUrl,
        )
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
