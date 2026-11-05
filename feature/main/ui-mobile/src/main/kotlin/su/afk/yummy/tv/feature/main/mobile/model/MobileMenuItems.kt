package su.afk.yummy.tv.feature.main.mobile.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.navigation.root.RootTab
import su.afk.yummy.tv.feature.main.presentation.R

/** Вкладки мобильной навигации (бар или рейка) в порядке показа. */
@Composable
internal fun mobileMenuItems(): List<MobileMenuItem<RootTab>> = listOf(
    MobileMenuItem(
        stringResource(R.string.main_tab_schedule),
        RootTab.SCHEDULE,
        Icons.Default.CalendarMonth,
    ),
    MobileMenuItem(
        stringResource(R.string.main_tab_home),
        RootTab.HOME,
        Icons.Default.Home,
        hasSeasonalDecor = true,
    ),
    MobileMenuItem(
        stringResource(R.string.main_tab_library),
        RootTab.LIBRARY,
        Icons.AutoMirrored.Filled.List,
    ),
)
