package su.afk.yummy.tv.feature.catalog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.components.YummyLoadingIndicator
import su.afk.yummy.tv.domain.home.model.HomeFeedItem
import su.afk.yummy.tv.feature.home.HomeState
import su.afk.yummy.tv.feature.home.mapper.toHomeEventOrNull
import su.afk.yummy.tv.feature.home.presentation.R
import su.afk.yummy.tv.feature.home.view.HomeCarousel

@Composable
fun CatalogTvScreen(
    state: HomeState.State,
    onEvent: (HomeState.Event) -> Unit,
) {
    LaunchedEffect(Unit) {
        onEvent(HomeState.Event.ScreenResumed)
    }

    val feed = state.feed
    Column(modifier = Modifier.fillMaxSize().padding(top = 32.dp)) {
        when {
            feed == null && state.isLoading -> YummyLoadingIndicator()
            feed == null -> Text(
                text = stringResource(R.string.home_load_error),
                style = MaterialTheme.typography.titleLarge,
            )
            else -> {
                Text(
                    text = stringResource(R.string.home_season_title),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(horizontal = 48.dp, vertical = 20.dp),
                )
                HomeCarousel(
                    items = feed.heroItems,
                    onItemSelected = { _: String, item: HomeFeedItem ->
                        item.action.toHomeEventOrNull()?.let(onEvent)
                    },
                    sectionKey = "catalog_seasonal",
                )
            }
        }
    }
}
