package su.afk.yummy.tv.feature.catalog.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.components.YummyLoadingIndicator
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileBottomBarDefaults
import su.afk.yummy.tv.feature.catalog.SeasonalCatalogState
import su.afk.yummy.tv.feature.home.mobile.view.HomeHeroCarousel
import su.afk.yummy.tv.feature.home.presentation.R

@Composable
fun CatalogMobileScreen(
    state: SeasonalCatalogState.State,
    onEvent: (SeasonalCatalogState.Event) -> Unit,
) {
    val feed = state.feed
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            feed == null && state.isLoading -> YummyLoadingIndicator()
            feed == null -> Column(modifier = Modifier.padding(24.dp)) {
                Text(text = stringResource(R.string.home_load_error))
                TextButton(onClick = { onEvent(SeasonalCatalogState.Event.RetrySelected) }) {
                    Text(text = stringResource(su.afk.yummy.tv.core.designsystem.R.string.common_retry))
                }
            }
            feed.heroItems.isEmpty() -> Text(
                text = stringResource(R.string.home_season_title),
                modifier = Modifier.padding(24.dp),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    bottom = MobileBottomBarDefaults.contentBottomPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Column(modifier = Modifier.padding(top = 20.dp)) {
                        Text(
                            text = stringResource(R.string.home_season_title),
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                        HomeHeroCarousel(
                            items = feed.heroItems,
                            onItemSelected = { item ->
                                onEvent(SeasonalCatalogState.Event.ItemSelected(item.action))
                            },
                        )
                    }
                }
            }
        }
    }
}
