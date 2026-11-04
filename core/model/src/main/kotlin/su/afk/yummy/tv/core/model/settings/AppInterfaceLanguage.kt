package su.afk.yummy.tv.core.model.settings

/**
 * Язык интерфейса приложения. SYSTEM означает использование языка устройства.
 *
 * Язык контента Yani хранится отдельно в [YaniContentLanguage].
 */
enum class AppInterfaceLanguage(val localeTag: String) {
    SYSTEM(""),
    ENGLISH("en"),
    RUSSIAN("ru"),
    UKRAINIAN("uk"),
    ;

    companion object {
        fun fromLocaleTags(tags: String?): AppInterfaceLanguage =
            entries.firstOrNull { it.localeTag.isNotEmpty() && it.localeTag.equals(tags?.substringBefore(',')?.substringBefore('-'), ignoreCase = true) }
                ?: SYSTEM
    }
}
