package su.afk.yummy.tv.feature.main.mobile.view

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgeDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.components.AppBrandIcon

@Composable
internal fun MobileGlobalHeader(
    collapsed: Boolean,
    unreadNotificationsCount: Int,
    onNotificationsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val height = animateDpAsState(if (collapsed) 48.dp else 64.dp, label = "header-height")
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .height(height.value)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Brand mark is intentionally decorative and non-interactive.
            AppBrandIcon(modifier = Modifier.size(36.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    IconButton(onClick = onNotificationsClick) {
                        Icon(
                            Icons.Filled.Notifications,
                            contentDescription = "Notifications",
                        )
                    }
                    if (unreadNotificationsCount > 0) {
                        Badge(
                            modifier = Modifier.align(Alignment.TopEnd).padding(top = 5.dp, end = 5.dp),
                            containerColor = BadgeDefaults.containerColor,
                        ) {
                            androidx.compose.material3.Text(
                                unreadNotificationsCount.coerceAtMost(99).toString(),
                            )
                        }
                    }
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = "Settings",
                    )
                }
                IconButton(onClick = onSearchClick) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = "Search",
                    )
                }
            }
        }
    }
}
