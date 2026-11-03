package su.afk.yummy.tv.data.search.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.core.network.yani.YaniHttpClientProvider
import su.afk.yummy.tv.core.preferences.api.YaniAccountSettingsStore
import su.afk.yummy.tv.core.storage.search.SearchStorage
import su.afk.yummy.tv.data.search.network.YaniSearchApi
import su.afk.yummy.tv.data.search.repository.DataStoreSearchHistoryRepository
import su.afk.yummy.tv.data.search.repository.YaniSearchRepository
import su.afk.yummy.tv.data.search.storage.searchHistoryDataStore
import su.afk.yummy.tv.domain.search.repository.SearchHistoryRepository
import su.afk.yummy.tv.domain.search.repository.SearchRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SearchDataModule {

    @Provides
    @Singleton
    fun provideYaniSearchApi(clientProvider: YaniHttpClientProvider): YaniSearchApi =
        YaniSearchApi(clientProvider)

    @Provides
    @Singleton
    fun provideSearchRepository(
        api: YaniSearchApi,
        searchStorage: SearchStorage,
        settingsStore: YaniAccountSettingsStore,
    ): SearchRepository =
        YaniSearchRepository(api, searchStorage, settingsStore)

    @Provides
    @Singleton
    fun provideSearchHistoryRepository(
        @ApplicationContext context: Context,
    ): SearchHistoryRepository = DataStoreSearchHistoryRepository(context.searchHistoryDataStore)
}
