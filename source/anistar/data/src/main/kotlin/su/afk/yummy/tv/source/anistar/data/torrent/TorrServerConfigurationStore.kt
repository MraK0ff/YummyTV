package su.afk.yummy.tv.source.anistar.data.torrent

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores the non-sensitive TorrServer endpoint. Credentials intentionally remain in memory:
 * they must not be written to plain SharedPreferences.
 */
interface TorrServerConfigurationStore {
    fun load(): TorrServerConfiguration
    fun saveBaseUrl(baseUrl: String)
}

@Singleton
class AndroidTorrServerConfigurationStore @Inject constructor(
    @param:ApplicationContext context: Context,
) : TorrServerConfigurationStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun load(): TorrServerConfiguration {
        val baseUrl = preferences.getString(BASE_URL_KEY, null)
            ?.takeIf(String::isNotBlank)
            ?: DEFAULT_BASE_URL
        return TorrServerConfiguration(baseUrl = baseUrl)
    }

    override fun saveBaseUrl(baseUrl: String) {
        preferences.edit().putString(BASE_URL_KEY, baseUrl).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "anistar_torrserver"
        const val BASE_URL_KEY = "base_url"
        const val DEFAULT_BASE_URL = "http://127.0.0.1:8090"
    }
}
