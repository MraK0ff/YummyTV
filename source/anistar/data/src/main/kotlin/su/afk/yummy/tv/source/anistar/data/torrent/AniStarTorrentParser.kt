package su.afk.yummy.tv.source.anistar.data.torrent

import java.net.URI

/**
 * Толерантный HTML-парсер списка и страницы раздачи AniStar.
 * Селекторы ограничены контейнером .torrent, отсутствующие поля остаются null.
 */
internal object AniStarTorrentParser {
    private val torrentBlocks = Regex(
        """<([a-z][a-z0-9]*)\b[^>]*class\s*=\s*["'][^"']*\btorrent\b[^"']*["'][^>]*>(.*?)</\1\s*>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val href = Regex("""href\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    private val magnet = Regex("""magnet:\?[^"'\s<>]+""", RegexOption.IGNORE_CASE)
    private val download = Regex(
        """(?:href|data-url)\s*=\s*["']([^"']+\.torrent(?:\?[^"']*)?)["']""",
        RegexOption.IGNORE_CASE,
    )
    private val title = Regex("""class\s*=\s*["'][^"']*info_d1[^"']*["'][^>]*>(.*?)<""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val size = Regex("""Размер\s*:\s*<[^>]*>\s*(.*?)\s*<""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val seeders = Regex("""class\s*=\s*["'][^"']*li_distribute[^"']*["'][^>]*>\s*(.*?)\s*<""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val leechers = Regex("""class\s*=\s*["'][^"']*li_swing[^"']*["'][^>]*>\s*(.*?)\s*<""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val downloads = Regex("""class\s*=\s*["'][^"']*li_download[^"']*["'][^>]*>\s*(.*?)\s*<""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val tags = Regex("""<[^>]+>""")
    private val whitespace = Regex("""\s+""")

    fun parseList(html: String, pageUrl: String): List<AniStarTorrent> =
        torrentBlocks.findAll(html).mapNotNull { match ->
            val block = match.groupValues[2]
            val link = href.find(block)?.groupValues?.get(1)?.let { resolve(pageUrl, decode(it)) } ?: return@mapNotNull null
            val name = title.find(block)?.groupValues?.get(1)?.let(::plainText)
                ?: href.find(block)?.groupValues?.get(1)?.let(::decode)
                ?: return@mapNotNull null
            AniStarTorrent(
                title = name,
                url = link,
                size = size.find(block)?.groupValues?.get(1)?.let(::plainText)?.takeIf(String::isNotBlank),
                seeders = seeders.find(block)?.groupValues?.get(1)?.let(::integer),
                leechers = leechers.find(block)?.groupValues?.get(1)?.let(::integer),
                downloads = downloads.find(block)?.groupValues?.get(1)?.let(::integer),
            )
        }.toList()

    fun parseTorrentPage(html: String, pageUrl: String): AniStarTorrentPage {
        val magnetUri = magnet.find(html)?.value?.let(::decode)
        val torrentUrl = download.find(html)?.groupValues?.get(1)?.let(::decode)?.let { resolve(pageUrl, it) }
        val pageTitle = Regex("""<title[^>]*>(.*?)</title>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .find(html)?.groupValues?.get(1)?.let(::plainText)
        return AniStarTorrentPage(pageTitle, magnetUri, torrentUrl)
    }

    private fun integer(value: String): Int? = plainText(value)
        .replace(Regex("""[^\d]"""), "")
        .toIntOrNull()

    private fun plainText(value: String): String = decode(tags.replace(value, " "))
        .replace(whitespace, " ")
        .trim()

    private fun resolve(base: String, value: String): String = runCatching {
        URI(base).resolve(value).toString()
    }.getOrDefault(value)

    private fun decode(value: String): String = value
        .replace("&amp;", "&", ignoreCase = true)
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        .replace("&nbsp;", " ")
        .replace(Regex("""&#(\d+);""")) { m ->
            m.groupValues[1].toIntOrNull()?.takeIf { it in 0..0x10FFFF }?.let { String(Character.toChars(it)) } ?: m.value
        }
}
