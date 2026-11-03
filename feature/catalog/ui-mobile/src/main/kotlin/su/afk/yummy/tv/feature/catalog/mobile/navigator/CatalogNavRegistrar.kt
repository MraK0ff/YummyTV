package su.afk.yummy.tv.feature.catalog.mobile.navigator

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.baseScreen.ScreenNavigator
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.feature.catalog.IMobileCatalogEntry
import su.afk.yummy.tv.feature.catalog.SeasonalCatalogViewModel
import su.afk.yummy.tv.feature.catalog.mobile.CatalogMobileScreen
import su.afk.yummy.tv.feature.catalog.navigator.CatalogDestination
import javax.inject.Inject

class CatalogNavRegistrar @Inject constructor() : IMobileCatalogEntry {
    override fun register(builder: EntryProviderScope<NavKey>, nav: INavigationManager) =
        with(builder) {
            entry<CatalogDestination> { destination ->
                val viewModel = hiltViewModel<SeasonalCatalogViewModel>()
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    CatalogMobileScreen(state = state, onEvent = onEvent)
                }
            }
        }
}
