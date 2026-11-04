package su.afk.yummy.tv.feature.catalog.mobile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import su.afk.yummy.tv.domain.catalog.updates.UpdatesFeedItem
import su.afk.yummy.tv.feature.catalog.UpdatesFeedViewModel
import java.text.DateFormat
import java.util.Date

@Composable
fun UpdatesFeedScreen(viewModel: UpdatesFeedViewModel) {
    val state by viewModel.state.collectAsState()
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Обновления", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = viewModel::refresh, enabled = !state.isLoading) { Text("Обновить") }
        }
        when {
            state.isLoading && state.items.isEmpty() -> CircularProgressIndicator(
                Modifier.align(Alignment.CenterHorizontally).padding(24.dp),
            )
            state.errorMessage != null && state.items.isEmpty() -> Column(
                Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(state.errorMessage.orEmpty())
                Button(onClick = viewModel::refresh) { Text("Повторить") }
            }
            state.items.isEmpty() -> Text("Подходящие тайтлы не найдены", Modifier.padding(24.dp))
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.items, key = UpdatesFeedItem::anixartId) { item ->
                    Card(Modifier.fillMaxWidth().clickable { viewModel.openAnime(item.animeId) }) {
                        Row(
                            Modifier.fillMaxWidth().padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            AsyncImage(
                                model = item.posterUrl,
                                contentDescription = null,
                                modifier = Modifier.size(width = 76.dp, height = 108.dp),
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(item.title, style = MaterialTheme.typography.titleMedium)
                                item.year?.let { Text(it.toString(), style = MaterialTheme.typography.bodySmall) }
                                item.episodesReleased?.let { released ->
                                    Text(
                                        if (item.episodesTotal != null) "Серии: $released / ${item.episodesTotal}"
                                        else "Вышло серий: $released",
                                    )
                                }
                                item.lastUpdateAt?.let {
                                    Text(
                                        "Обновлено: ${DateFormat.getDateTimeInstance().format(Date(it))}",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        if (state.isLoading && state.items.isNotEmpty()) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally).padding(8.dp))
        }
        state.errorMessage?.takeIf { state.items.isNotEmpty() }?.let { Text(it) }
    }
}
