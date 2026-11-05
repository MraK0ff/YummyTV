package su.afk.yummy.tv.feature.main.mobile

import su.afk.yummy.tv.core.designsystem.seasonal.SeasonalPrompt
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import su.afk.yummy.tv.core.common.coroutines.AppClock
import su.afk.yummy.tv.core.designsystem.baseScreen.ScreenNavigator
import su.afk.yummy.tv.core.designsystem.components.rememberGlobalToastState
import su.afk.yummy.tv.core.designsystem.dialog.ConfirmDialogHost
import su.afk.yummy.tv.core.designsystem.locals.LocalIsOffline
import su.afk.yummy.tv.core.designsystem.locals.LocalLibraryListStatuses
import su.afk.yummy.tv.core.designsystem.locals.LocalLibraryUserRatings
import su.afk.yummy.tv.core.designsystem.locals.LocalNowMillis
import su.afk.yummy.tv.core.designsystem.locals.LocalPosterCardSize
import su.afk.yummy.tv.core.designsystem.locals.LocalPosterQuality
import su.afk.yummy.tv.core.designsystem.mobile.bar.LocalMobileBottomBarUpFocusRequester
import su.afk.yummy.tv.core.designsystem.seasonal.LocalSeasonalDecorationsEnabled
import su.afk.yummy.tv.core.designsystem.theme.YummyTvTheme
import su.afk.yummy.tv.core.navigation.host.AppNavHost
import su.afk.yummy.tv.core.navigation.manager.BackStackHost
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.navigation.root.RootTab
import su.afk.yummy.tv.core.navigation.scene.FullscreenDestination
import su.afk.yummy.tv.feature.main.MainState
import su.afk.yummy.tv.feature.account.navigator.AccountNotificationsDestination
import su.afk.yummy.tv.feature.catalog.ICatalogSearchOpener
import su.afk.yummy.tv.feature.main.MainViewModel
import su.afk.yummy.tv.feature.main.api.MainGraph
import su.afk.yummy.tv.feature.main.mobile.model.mobileMenuItems
import su.afk.yummy.tv.feature.main.mobile.navigation.MobileNavigationHolder
import su.afk.yummy.tv.feature.main.mobile.navigation.mobileNavPopTransitionSpec
import su.afk.yummy.tv.feature.main.mobile.navigation.mobileNavTransitionSpec
import su.afk.yummy.tv.feature.main.mobile.navigation.rememberMobileListDetailSceneStrategy
import su.afk.yummy.tv.feature.main.mobile.view.MobileMainScaffold
import su.afk.yummy.tv.feature.update.navigator.UpdateDestination
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MobileMainGraph @Inject internal constructor(
    private val appClock: AppClock,
    private val navManager: INavigationManager,
    private val backStackHost: BackStackHost,
    private val navigationHolder: MobileNavigationHolder,
    private val catalogSearchOpener: ICatalogSearchOpener,
) : MainGraph {

    @Composable
    override fun MainGraph() {
        val viewModel: MainViewModel = hiltViewModel()
        val currentDestination = navManager.backStack.lastOrNull()
        val atTabRoot = navManager.appBackStack.isEmpty() && navManager.backStack.size <= 1
        val isRequiredUpdateDestination =
            currentDestination is UpdateDestination && currentDestination.required
        val isFullscreenDestination = currentDestination is FullscreenDestination
        val showRail = !isRequiredUpdateDestination && !isFullscreenDestination

        ScreenNavigator(viewModel) { state, effect, onEvent ->
            val accountSettingsFocusRequester = remember { FocusRequester() }
            val toast = rememberGlobalToastState()
            LaunchedEffect(effect) {
                effect.collect { eff ->
                    when (eff) {
                        is MainState.Effect.ShowToast -> toast.show(eff.message)
                    }
                }
            }

            YummyTvTheme(
                appTheme = state.appTheme,
                backgroundStyle = state.backgroundStyle,
                isTelevision = false,
            ) {
                ConfirmDialogHost(isTelevision = false)
                SeasonalPrompt(
                    theme = state.seasonalPrompt,
                    isTelevision = false,
                    ready = atTabRoot && !isRequiredUpdateDestination && !isFullscreenDestination,
                    onAnswer = { disable -> onEvent(MainState.Event.SeasonalPromptAnswered(disable)) },
                )
                CompositionLocalProvider(
                    LocalPosterQuality provides state.posterQuality,
                    LocalPosterCardSize provides state.posterCardSize,
                    LocalIsOffline provides !state.isOnline,
                    LocalSeasonalDecorationsEnabled provides state.seasonalDecorationsEnabled,
                    LocalNowMillis provides appClock::nowMillis,
                    LocalLibraryListStatuses provides state.libraryListStatuses,
                    LocalLibraryUserRatings provides state.libraryUserRatings,
                    LocalMobileBottomBarUpFocusRequester provides accountSettingsFocusRequester.takeIf {
                        atTabRoot && navManager.currentRoot == RootTab.ACCOUNT
                    },
                ) {
                    MobileMainScaffold(
                        selectedDestination = navManager.currentRoot,
                        menuItems = mobileMenuItems(),
                        showBottomBar = atTabRoot && !isRequiredUpdateDestination &&
                            !isFullscreenDestination,
                        showRail = showRail,
                        showGlobalHeader = atTabRoot && !isRequiredUpdateDestination && !isFullscreenDestination,
                        avatarUrl = if (state.isYaniSignedIn) state.yaniAvatarUrl else "",
                        unreadNotificationsCount = state.unreadNotificationsCount,
                        onSearchSelected = { catalogSearchOpener.openSearch() },
                        onProfileSelected = {
                            onEvent(MainState.Event.RootSelected(RootTab.ACCOUNT, popToRootOnReselect = true))
                        },
                        onNotificationsSelected = {
                            navManager.navigate(AccountNotificationsDestination)
                        },
                        onSettingsSelected = {
                            onEvent(MainState.Event.RootSelected(RootTab.SETTINGS, popToRootOnReselect = true))
                        },
                        isSearchSelected = navManager.currentRoot == RootTab.CATALOG,
                        isProfileSelected = navManager.currentRoot == RootTab.ACCOUNT,
                        isNotificationsSelected = currentDestination is AccountNotificationsDestination,
                        isSettingsSelected = navManager.currentRoot == RootTab.SETTINGS,
                        onDestinationSelected = { root ->
                            onEvent(MainState.Event.RootSelected(root, popToRootOnReselect = true))
                        },
                        toastMessage = toast.message,
                        showSeasonalOverlay = !isFullscreenDestination,
                    ) {
                        AppNavHost(
                            navManager = navManager,
                            backStackHost = backStackHost,
                            registrars = navigationHolder.registrars,
                            modifier = Modifier.fillMaxSize(),
                            extraSceneStrategies = listOf(
                                rememberMobileListDetailSceneStrategy(currentDestination),
                            ),
                            transitionSpec = remember(state.navTransitionStyle) {
                                mobileNavTransitionSpec(state.navTransitionStyle)
                            },
                            popTransitionSpec = remember(state.navTransitionStyle) {
                                mobileNavPopTransitionSpec(state.navTransitionStyle)
                            },
                        )
                    }
                }
            }
        }
    }
}
