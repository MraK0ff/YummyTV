package su.afk.yummy.tv.source.anistar.data.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnistarSearchUtilsTest {

    @Test
    fun \`finds DLE news id by Russian or alternate title\`() {
        val html = """
            <div id="dle-content">
              <div class="shortstory">
                <a href="/10933-krasnaya-reka.html">Красная река / Sora wa Akai Kawa no Hotori</a>
                <span>Год выпуска: 2026</span>
              </div>
            </div>
        """.trimIndent()

        val result = findBestAnistarCandidate(html, listOf("Sora wa Akai Kawa no Hotori"), 2026)

        assertEquals(10933L, result?.newsId)
        assertEquals(2026, result?.year)
    }

    @Test
    fun \`does not return unrelated or adult category results\`() {
        val adultResult = """
            <div class="shortstory">
              <a href="/10-some-hentai-title.html">Frieren Hentai</a>
              <span>Категория: Hentai</span>
            </div>
        """.trimIndent()
        val unrelatedResult = """
            <div class="shortstory">
              <a href="/11-unrelated.html">Другой сериал</a>
            </div>
        """.trimIndent()

        assertNull(findBestAnistarCandidate(adultResult + "x".repeat(1200) + unrelatedResult, listOf("Фрирен"), 2023))
    }

    @Test
    fun \`accepts absolute article links and query style ids\`() {
        val html = """
            <a href="https://v31.astar.bz/2658-frieren.html">Фрирен / Frieren</a>
            <a href="/index.php?newsid=2659">Frieren</a>
        """.trimIndent()

        val result = findBestAnistarCandidate(html, listOf("Фрирен"), null)

        assertEquals(2658L, result?.newsId)
    }
}
