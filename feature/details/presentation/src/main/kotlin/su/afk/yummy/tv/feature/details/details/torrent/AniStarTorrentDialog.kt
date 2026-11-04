package su.afk.yummy.tv.feature.details.details.torrent

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PasswordVisualTransformation
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.details.details.DetailsState

@Composable
fun AniStarTorrentDialog(
    state: DetailsState.State,
    onEvent: (DetailsState.Event) -> Unit,
) {
    var serverUrl by rememberSaveable { mutableStateOf("http://127.0.0.1:8090") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    if (!state.showAniStarTorrentDialog) return

    AlertDialog(
        onDismissRequest = { onEvent(DetailsState.Event.AniStarTorrentsDismissed) },
        title = { Text("AniStar Torrents") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = { Text("TorrServer URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password (optional)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        onEvent(DetailsState.Event.TorrServerConfigurationSelected(serverUrl, username, password))
                    }) { Text("Save server") }
                    TextButton(onClick = {
                        onEvent(DetailsState.Event.TorrServerConfigurationSelected(serverUrl, username, password))
                        onEvent(DetailsState.Event.TorrServerConnectionCheckSelected)
                    }) { Text("Test") }
                }
                state.torrServerStatus?.let { Text(it) }
                HorizontalDivider()
                if (state.isAniStarTorrentLoading) CircularProgressIndicator()
                state.aniStarTorrentError?.let { Text(it) }
                if (state.aniStarTorrentFiles.isNotEmpty()) {
                    Text(state.selectedTorrentTitle.orEmpty())
                    LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
                        items(state.aniStarTorrentFiles, key = { "${it.hash}-${it.index}" }) { file ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onEvent(DetailsState.Event.AniStarTorrentFileSelected(file)) }
                                    .padding(vertical = 8.dp),
                            ) {
                                Text(file.path)
                                Text(formatBytes(file.sizeBytes))
                            }
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
                        items(state.aniStarTorrents, key = { it.pageUrl }) { torrent ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onEvent(DetailsState.Event.AniStarTorrentSelected(torrent)) }
                                    .padding(vertical = 8.dp),
                            ) {
                                Text(torrent.title)
                                Text(
                                    listOfNotNull(
                                        torrent.size,
                                        torrent.seeders?.let { "Seeds: $it" },
                                        torrent.leechers?.let { "Peers: $it" },
                                    ).joinToString(" · "),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onEvent(DetailsState.Event.AniStarTorrentsSelected) }) {
                Text("Search AniStar")
            }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(DetailsState.Event.AniStarTorrentsDismissed) }) {
                Text("Close")
            }
        },
    )
}

private fun formatBytes(bytes: Long): String = when {
    bytes <= 0 -> ""
    bytes >= 1024L * 1024 * 1024 -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
    bytes >= 1024L * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
    else -> "%.0f KB".format(bytes / 1024.0)
}
