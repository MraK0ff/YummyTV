package su.afk.yummy.tv.source.anistar.data.utils

/**
 * Извлекает ID новости из стандартной ссылки DLE (/12345-title.html) и выбирает
 * наиболее похожий результат. Ссылки на "Смотреть", баннеры и посторонние блоки
 * отсекаются оценкой названия; категории Hentai исключены.
 */
internal data class AnistarSearchCandidate(
    val newsId: Long,
    val title: String,
    val year: Int?,
)

private val ANISTAR_ANCHOR = Regex(
    """<a\b[^>]*?href\s*=\s*(['"])(.*?)\1[^>]*>(.*?)</a>""",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
)
private val NEWS_ID_IN_PATH = Regex("""(?:^|/)(\d+)-[^/?#]*\.html(?:[?#]|$)""", RegexOption.IGNORE_CASE)
private val NEWS_ID_IN_QUERY = Regex("""(?:newsid|news_id)=(\d+)""", RegexOption.IGNORE_CASE)
private val YEAR_IN_CONTEXT = Regex("""(?:год\s+выпуска|год)\s*[:：]\s*((?:19|20)\d{2})""", RegexOption.IGNORE_CASE)
private val HENTAI_MARKER = Regex("""hentai|хентай""", RegexOption.IGNORE_CASE)
private val HTML_TAGS = Regex("""<[^>]+>""")
private val NON_WORD = Regex("""[^\p{L}\p{N}]+""")

internal fun findBestAnistarCandidate(
    html: String,
    titles: List<String>,
    expectedYear: Int?,
): AnistarSearchCandidate? {
    val normalizedTitles = titles.map(::normalizeTitle).filter(String::isNotBlank).distinct()
    if (normalizedTitles.isEmpty()) return null

    return ANISTAR_ANCHOR.findAll(html).mapNotNull { anchor ->
        val href = decodeHtml(anchor.groupValues[2]).trim()
        val newsId = NEWS_ID_IN_PATH.find(href)?.groupValues?.get(1)?.toLongOrNull()
            ?: NEWS_ID_IN_QUERY.find(href)?.groupValues?.get(1)?.toLongOrNull()
            ?: return@mapNotNull null
        if (newsId <= 0) return@mapNotNull null

        val title = decodeHtml(HTML_TAGS.replace(anchor.groupValues[3], " "))
            .replace(Regex("""\s+"""), " ")
            .trim()
        if (title.isBlank()) return@mapNotNull null

        // Проверяем локальный фрагмент карточки: категория другого результата
        // на той же странице не должна скрывать подходящий тайтл.
        val contextStart = (anchor.range.first - CONTEXT_RADIUS).coerceAtLeast(0)
        val contextEnd = (anchor.range.last + CONTEXT_RADIUS).coerceAtMost(html.length)
        val context = html.substring(contextStart, contextEnd)
        if (HENTAI_MARKER.containsMatchIn(context)) return@mapNotNull null

        val normalizedCandidate = normalizeTitle(title)
        val score = normalizedTitles.maxOfOrNull { similarity(it, normalizedCandidate) } ?: 0.0
        if (score < MIN_TITLE_SCORE) return@mapNotNull null

        val year = YEAR_IN_CONTEXT.find(context)?.groupValues?.get(1)?.toIntOrNull()
        val adjustedScore = score + when {
            expectedYear == null || year == null -> 0.0
            expectedYear == year -> YEAR_MATCH_BONUS
            else -> -YEAR_MISMATCH_PENALTY
        }
        if (adjustedScore < MIN_ADJUSTED_SCORE) return@mapNotNull null

        ScoredCandidate(AnistarSearchCandidate(newsId, title, year), adjustedScore)
    }.maxByOrNull { it.score }?.candidate
}

private data class ScoredCandidate(
    val candidate: AnistarSearchCandidate,
    val score: Double,
)

private fun similarity(expected: String, actual: String): Double {
    if (expected == actual) return 1.0
    if (expected.isBlank() || actual.isBlank()) return 0.0

    val expectedTokens = expected.split(' ').filter(String::isNotBlank).toSet()
    val actualTokens = actual.split(' ').filter(String::isNotBlank).toSet()
    val common = expectedTokens.intersect(actualTokens)
    if (common.isEmpty()) return 0.0

    val coverage = common.size.toDouble() / expectedTokens.size
    val jaccard = common.size.toDouble() / expectedTokens.union(actualTokens).size
    return when {
        coverage == 1.0 -> 0.88 + (jaccard * 0.1)
        coverage >= 0.8 -> coverage * 0.85 + jaccard * 0.15
        else -> coverage * 0.7 + jaccard * 0.3
    }
}

private fun normalizeTitle(value: String): String =
    decodeHtml(HTML_TAGS.replace(value, " "))
        .lowercase()
        .replace(NON_WORD, " ")
        .trim()
        .replace(Regex("""\s+"""), " ")

private fun decodeHtml(value: String): String = value
    .replace("&amp;", "&", ignoreCase = true)
    .replace("&quot;", "\"", ignoreCase = true)
    .replace("&#39;", "'", ignoreCase = true)
    .replace("&apos;", "'", ignoreCase = true)
    .replace("&nbsp;", " ", ignoreCase = true)
    .replace(Regex("""&#(\d+);""")) { match ->
        match.groupValues[1].toIntOrNull()?.takeIf { it in 0..0xFFFF }?.toChar()?.toString() ?: match.value
    }

private const val CONTEXT_RADIUS = 450
private const val MIN_TITLE_SCORE = 0.78
private const val MIN_ADJUSTED_SCORE = 0.75
private const val YEAR_MATCH_BONUS = 0.04
private const val YEAR_MISMATCH_PENALTY = 0.16
