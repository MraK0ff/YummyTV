package su.afk.yummy.tv.feature.settings.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.settings.HomeSectionKind
import su.afk.yummy.tv.feature.settings.SettingsState
import su.afk.yummy.tv.feature.settings.model.DetailsButtonMoveDirection
import su.afk.yummy.tv.feature.settings.presentation.R
import su.afk.yummy.tv.feature.settings.utils.restoreCategoryFocusOnLeft

@Composable
internal fun HomeSectionsPanel(
    state: SettingsState.State,
    upFocusRequester: FocusRequester,
    contentFocusRequester: FocusRequester?,
    onEvent: (SettingsState.Event) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        DetailsButtonOrderResetRow(
            onReset = { onEvent(SettingsState.Event.HomeSectionOrderReset) },
            label = stringResource(R.string.settings_home_sections_reset),
            hint = stringResource(R.string.settings_home_sections_reset_hint),
            modifier = Modifier
                .then(
                    if (contentFocusRequester != null) Modifier.focusRequester(contentFocusRequester)
                    else Modifier,
                )
                .restoreCategoryFocusOnLeft(upFocusRequester),
        )
        SettingsDivider()
        state.homeSectionOrder.forEachIndexed { index, section ->
            DetailsButtonOrderRow(
                label = stringResource(section.labelRes()),
                position = index + 1,
                canMoveUp = index > 0,
                canMoveDown = index < state.homeSectionOrder.lastIndex,
                onMoveUp = { onEvent(SettingsState.Event.HomeSectionMoved(section, DetailsButtonMoveDirection.UP)) },
                onMoveDown = { onEvent(SettingsState.Event.HomeSectionMoved(section, DetailsButtonMoveDirection.DOWN)) },
            )
            SettingsDivider()
        }
        SettingsSectionTitle(text = stringResource(R.string.settings_home_sections_visibility_title))
        HomeSectionKind.entries.forEach { section ->
            val enabled = if (section == HomeSectionKind.MY_NEW_EPISODES) {
                state.newEpisodesSectionEnabled
            } else {
                section !in state.hiddenHomeSections
            }
            ToggleRow(
                label = stringResource(section.labelRes()),
                hint = if (enabled) stringResource(R.string.settings_home_section_enabled)
                else stringResource(R.string.settings_disabled),
                enabled = enabled,
                onClick = {
                    if (section == HomeSectionKind.MY_NEW_EPISODES) {
                        onEvent(SettingsState.Event.NewEpisodesSectionToggled)
                    } else {
                        onEvent(SettingsState.Event.HomeSectionVisibilityToggled(section))
                    }
                },
                modifier = Modifier.restoreCategoryFocusOnLeft(upFocusRequester),
            )
            SettingsDivider()
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
