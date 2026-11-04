package su.afk.yummy.tv.feature.main.mobile.view

import su.afk.yummy.tv.core.designsystem.seasonal.SeasonalOverlay
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldLayout
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.components.GlobalToastOverlay
import su.afk.yummy.tv.core.designsystem.mobile.bar.LocalMobileBottomBarUpFocusRequester
import su.afk.yummy.tv.core.designsystem.mobile.bar.LocalMobileNavigationLayout
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileBottomBarDefaults
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileNavigationLayout
import su.afk.yummy.tv.feature.main.mobile.model.MobileMenuItem

@Composable
internal fun <T> MobileMainScaffold(
    selectedDestination: T,
    menuItems: List<MobileMenuItem<T>>,
    showBottomBar: Boolean,
    showRail: Boolean,
    showGlobalHeader: Boolean,
    avatarUrl: String,
    unreadNotificationsCount: Int,
    onProfileSelected: () -> Unit,
    onNotificationsSelected: () -> Unit,
    onSettingsSelected: () -> Unit,
    isProfileSelected: Boolean,
    isNotificationsSelected: Boolean,
    isSettingsSelected: Boolean,
    onDestinationSelected: (T) -> Unit,
    toastMessage: String?,
    showSeasonalOverlay: Boolean = true,
    content: @Composable () -> Unit,
) {
    val bottomBarUpFocusRequester = LocalMobileBottomBarUpFocusRequester.current
    val suiteType = NavigationSuiteScaffoldDefaults.navigationSuiteType(currentWindowAdaptiveInfoV2())
    val isRail = suiteType != NavigationSuiteType.NavigationBar &&
        suiteType != NavigationSuiteType.ShortNavigationBarCompact &&
        suiteType != NavigationSuiteType.ShortNavigationBarMedium
    val isVisible = if (isRail) showRail else showBottomBar
    val layout = when {
        !isVisible -> MobileNavigationLayout.Hidden
        isRail -> MobileNavigationLayout.Rail
        else -> MobileNavigationLayout.BottomBar
    }

    val suiteState = rememberNavigationSuiteScaffoldState()
    LaunchedEffect(isVisible) {
        if (isVisible) suiteState.show() else suiteState.hide()
    }

    val surface = MaterialTheme.colorScheme.surface
    NavigationSuiteScaffoldLayout(
        navigationSuiteType = if (isRail) NavigationSuiteType.NavigationRail else NavigationSuiteType.NavigationBar,
        state = suiteState,
        navigationSuite = {
            if (isRail) {
                MobileMainNavigationRail(
                    selectedDestination = selectedDestination,
                    menuItems = menuItems,
                    containerColor = surface,
                    onDestinationSelected = onDestinationSelected,
                )
            } else {
                MobileMainNavigationBar(
                    selectedDestination = selectedDestination,
                    menuItems = menuItems,
                    containerColor = surface,
                    upFocusRequester = bottomBarUpFocusRequester,
                    onDestinationSelected = onDestinationSelected,
                )
            }
        },
    ) {
        CompositionLocalProvider(LocalMobileNavigationLayout provides layout) {
            val barInsets = if (layout == MobileNavigationLayout.BottomBar) {
                WindowInsets.navigationBars.add(WindowInsets(bottom = MobileBottomBarDefaults.BarHeight))
            } else {
                WindowInsets(0.dp)
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .consumeWindowInsets(barInsets)
                    .background(MaterialTheme.colorScheme.background),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (showGlobalHeader) {
                        MobileGlobalHeader(
                            avatarUrl = avatarUrl,
                            unreadNotificationsCount = unreadNotificationsCount,
                            onProfileClick = onProfileSelected,
                            onNotificationsClick = onNotificationsSelected,
                            onSettingsClick = onSettingsSelected,
                            isProfileSelected = isProfileSelected,
                            isNotificationsSelected = isNotificationsSelected,
                            isSettingsSelected = isSettingsSelected,
                        )
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        content()
                        GlobalToastOverlay(
                            text = toastMessage,
                            modifier = Modifier.padding(bottom = MobileBottomBarDefaults.contentBottomPadding),
                        )
                    }
                }
                if (showSeasonalOverlay) SeasonalOverlay()
            }
        }
    }
}
