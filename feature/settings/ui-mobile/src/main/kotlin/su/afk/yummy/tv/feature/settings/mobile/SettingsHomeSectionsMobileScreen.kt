package su.afk.yummy.tv.feature.settings.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileTopBar
import su.afk.yummy.tv.core.designsystem.mobile.layout.mobileContentMaxWidth
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.feature.settings.HomeSectionKind
import su.afk.yummy.tv.feature.settings.SettingsState
import su.afk.yummy.tv.feature.settings.model.DetailsButtonMoveDirection
import su.afk.yummy.tv.feature.settings.mobile.view.DetailsOrderRow
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileSection
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileToggleRow
import su.afk.yummy.tv.feature.settings.presentation.R

@Preview(name = "Home sections", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SettingsHomeSectionsMobileScreenPreview() = ScreenPreviewTheme {
    SettingsHomeSectionsMobileScreen(SettingsState.State(), emptyFlow()) {}
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsHomeSectionsMobileScreen(
    state: SettingsState.State,
    effect: Flow<SettingsState.Effect>,
    onEvent: (SettingsState.Event) -> Unit,
) {
    val title = stringResource(R.string.settings_home_sections_title)
    BaseScreen(
        isScroll = false,
        contentModifier = Modifier.navigationBarsPadding(),
        customTopBar = {
            MobileTopBar(
                title = title,
                onBack = { onEvent(SettingsState.Event.BackSelected) },
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier.mobileContentMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                SettingsMobileSection {
                    Text(
                        text = stringResource(R.string.settings_home_sections_hint),
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    state.homeSectionOrder.forEachIndexed { index, section ->
                        DetailsOrderRow(
                            label = stringResource(section.labelRes()),
                            position = index + 1,
                            canMoveUp = index > 0,
                            canMoveDown = index < state.homeSectionOrder.lastIndex,
                            onMoveUp = {
                                onEvent(SettingsState.Event.HomeSectionMoved(section, DetailsButtonMoveDirection.UP))
                            },
                            onMoveDown = {
                                onEvent(SettingsState.Event.HomeSectionMoved(section, DetailsButtonMoveDirection.DOWN))
                            },
                        )
                    }
                }
            }
            item {
                SettingsMobileSection(title = stringResource(R.string.settings_home_sections_visibility_title)) {
                    HomeSectionKind.entries.forEach { section ->
                        val enabled = if (section == HomeSectionKind.MY_NEW_EPISODES) {
                            state.newEpisodesSectionEnabled
                        } else {
                            section !in state.hiddenHomeSections
                        }
                        SettingsMobileToggleRow(
                            label = stringResource(section.labelRes()),
                            hint = if (enabled) {
                                stringResource(R.string.settings_home_section_enabled)
                            } else {
                                stringResource(R.string.settings_disabled)
                            },
                            enabled = enabled,
                            onClick = {
                                if (section == HomeSectionKind.MY_NEW_EPISODES) {
                                    onEvent(SettingsState.Event.NewEpisodesSectionToggled)
                                } else {
                                    onEvent(SettingsState.Event.HomeSectionVisibilityToggled(section))
                                }
                            },
                        )
                    }
                }
            }
            item {
                SettingsMobileSection {
                    androidx.compose.material3.OutlinedButton(
                        onClick = { onEvent(SettingsState.Event.HomeSectionOrderReset) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.settings_home_sections_reset))
                    }
                }
            }
        }
    }
}

private fun HomeSectionKind.labelRes(): Int = when (this) {
    HomeSectionKind.SEASON_ANIME -> R.string.settings_home_section_season_anime
    HomeSectionKind.CONTINUE_WATCHING -> R.string.settings_home_section_continue_watching
    HomeSectionKind.MY_NEW_EPISODES -> R.string.settings_home_section_my_new_episodes
    HomeSectionKind.NEW_RELEASES -> R.string.settings_home_section_new_releases
    HomeSectionKind.NEW_TITLES -> R.string.settings_home_section_new_titles
    HomeSectionKind.RECOMMENDATIONS -> R.string.settings_home_section_recommendations
    HomeSectionKind.ANNOUNCEMENTS -> R.string.settings_home_section_announcements
    HomeSectionKind.COLLECTIONS -> R.string.settings_home_section_collections
    HomeSectionKind.BLOGGER_VIDEOS -> R.string.settings_home_section_blogger_videos
    HomeSectionKind.MORE -> R.string.settings_home_section_more
}
