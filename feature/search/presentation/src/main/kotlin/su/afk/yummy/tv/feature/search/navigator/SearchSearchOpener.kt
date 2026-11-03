package su.afk.yummy.tv.feature.search.navigator

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.navigation.root.RootTab
import su.afk.yummy.tv.feature.search.ISearchOpener
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class SearchOpener @Inject constructor(
    private val nav: INavigationManager,
) : ISearchOpener {
    private val pending = MutableStateFlow(false)

    override val isSearchFocusPending: StateFlow<Boolean> = pending.asStateFlow()

    override fun openSearch() {
        pending.value = true
        // Если каталог уже открыт, возвращаемся к корню вкладки: поле поиска живёт там.
        nav.switchRoot(RootTab.SEARCH, reselectPopToRoot = true)
    }

    override fun consumeSearchFocus() {
        pending.value = false
    }
}
