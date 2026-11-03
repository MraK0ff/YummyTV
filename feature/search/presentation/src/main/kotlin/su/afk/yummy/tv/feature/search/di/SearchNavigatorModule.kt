package su.afk.yummy.tv.feature.search.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.search.ISearchNavigator
import su.afk.yummy.tv.feature.search.ISearchOpener
import su.afk.yummy.tv.feature.search.navigator.SearchNavigator
import su.afk.yummy.tv.feature.search.navigator.SearchSearchOpener
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface SearchNavigatorModule {

    @Binds
    @Singleton
    fun bindSearchNavigator(impl: SearchNavigator): ISearchNavigator

    @Binds
    @Singleton
    fun bindSearchSearchOpener(impl: SearchSearchOpener): ISearchOpener
}
