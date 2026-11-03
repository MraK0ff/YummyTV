package su.afk.yummy.tv.feature.main.mobile.view

import su.afk.yummy.tv.core.designsystem.seasonal.SeasonalOverlay
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

/**
 * Корневой каркас мобильного UI: на компактной ширине — нижний бар, на планшете, развёрнутом
 * складном и в широком окне — боковая рейка. [showBottomBar] и [showRail] задаются раздельно:
 * бар занимает высоту контента и уходит вне корня таба, рейка остаётся рядом с деталями.
 */
@Composable
internal fun <T> MobileMainScaffold(
    selectedDestination: T,
    menuItems: List<MobileMenuItem<T>>,
    showBottomBar: Boolean,
    showRail: Boolean,
    onDestinationSelected: (T) -> Unit,
    toastMessage: String?,
    unreadNotificationsCount: Int,
    onNotificationsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSearchClick: () -> Unit,
    showHeader: Boolean = true,
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
        navigationSuiteType = if (isRail) {
            NavigationSuiteType.NavigationRail
        } else {
            NavigationSuiteType.NavigationBar
        },
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
            // Низ контента уже занят баром вместе с системным инсетом: без консьюма imePadding
            // в экранах табов отступал бы на полную высоту клавиатуры и оставлял пустую полосу.
            val barInsets = if (layout == MobileNavigationLayout.BottomBar) {
                WindowInsets.navigationBars.add(WindowInsets(bottom = MobileBottomBarDefaults.BarHeight))
            } else {
                WindowInsets(0.dp)
            }
            var collapsed by remember { mutableStateOf(false) }
            var scrollAccumulator by remember { mutableFloatStateOf(0f) }
            val scrollConnection = remember {
                object : NestedScrollConnection {
                    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                        val dy = available.y
                        if (dy > 0f) {
                            scrollAccumulator = 0f
                            collapsed = false
                        } else if (dy < 0f) {
                            scrollAccumulator += -dy
                            if (scrollAccumulator > 18f) {
                                collapsed = true
                                scrollAccumulator = 0f
                            }
                        }
                        return Offset.Zero
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .consumeWindowInsets(barInsets)
                    .background(MaterialTheme.colorScheme.background)
                    .nestedScroll(scrollConnection),
            ) {
                if (showHeader) MobileGlobalHeader(
                    collapsed = collapsed,
                    unreadNotificationsCount = unreadNotificationsCount,
                    onNotificationsClick = onNotificationsClick,
                    onSettingsClick = onSettingsClick,
                    onSearchClick = onSearchClick,
                    modifier = Modifier.statusBarsPadding(),
                )
                Box(modifier = Modifier.weight(1f)) {
                    content()
                    if (showSeasonalOverlay) SeasonalOverlay()
                    GlobalToastOverlay(
                        text = toastMessage,
                        modifier = Modifier.padding(bottom = MobileBottomBarDefaults.contentBottomPadding),
                    )
                }
            }
        }
    }
}
