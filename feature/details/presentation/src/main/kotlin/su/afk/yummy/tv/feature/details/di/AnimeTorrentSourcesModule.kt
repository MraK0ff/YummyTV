package su.afk.yummy.tv.feature.details.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import su.afk.yummy.tv.domain.details.torrent.AnimeTorrentSource

/** Optional torrent-source modules may contribute zero or more implementations. */
@Module
@InstallIn(SingletonComponent::class)
internal interface AnimeTorrentSourcesModule {
    @Multibinds
    fun animeTorrentSources(): Set<@JvmSuppressWildcards AnimeTorrentSource>
}
