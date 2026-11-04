package su.afk.yummy.tv.feature.settings.mobile.view.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.model.settings.LibraryContinueWatchingCardSize
import su.afk.yummy.tv.core.model.settings.PosterCardSize
import su.afk.yummy.tv.core.model.settings.TitleCardLayout
import su.afk.yummy.tv.feature.settings.HomeSectionKind
import su.afk.yummy.tv.feature.settings.SettingsState
import su.afk.yummy.tv.feature.settings.model.DetailsButtonMoveDirection
import su.afk.yummy.tv.feature.settings.common.utils.hint
import su.afk.yummy.tv.feature.settings.common.utils.icon
import su.afk.yummy.tv.feature.settings.common.utils.label
import su.afk.yummy.tv.feature.settings.common.utils.newEpisodesSourcesValue
import su.afk.yummy.tv.feature.settings.mobile.model.SettingsMobilePicker
import su.afk.yummy.tv.feature.settings.mobile.utils.hint
import su.afk.yummy.tv.feature.settings.mobile.utils.label
import su.afk.yummy.tv.feature.settings.mobile.view.DetailsOrderRow
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileActionRow
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileIconChoice
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileIconChoiceRow
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileNavigationRow
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileOptionRow
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileSection
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileToggleRow
import su.afk.yummy.tv.feature.settings.common.utils.enabledHint
import su.afk.yummy.tv.feature.settings.presentation.R

@Composable
internal fun SettingsMobileAppearanceContent(
    state: SettingsState.State,
    onEvent: (SettingsState.Event) -> Unit,
    onPickerRequested: (SettingsMobilePicker) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        SettingsMobileSection(title = stringResource(R.string.settings_section_general_appearance)) {
            SettingsMobileOptionRow(
                label = stringResource(R.string.settings_tab_theme),
                value = state.appTheme.label(),
                hint = state.appTheme.hint(),
                onClick = { onPickerRequested(SettingsMobilePicker.THEME) },
            )
            SettingsMobileOptionRow(
                label = stringResource(R.string.settings_tab_background),
                value = state.backgroundStyle.label(),
                hint = state.backgroundStyle.hint(),
                onClick = { onPickerRequested(SettingsMobilePicker.BACKGROUND) },
            )
            SettingsMobileOptionRow(
                label = stringResource(R.string.settings_nav_transition_title),
                value = state.navTransitionStyle.label(),
                hint = state.navTransitionStyle.hint(),
                onClick = { onPickerRequested(SettingsMobilePicker.NAV_TRANSITION) },
            )
            SettingsMobileToggleRow(
                label = stringResource(R.string.settings_seasonal_decorations),
                hint = if (state.seasonalDecorationsEnabled) {
                    state.seasonalTheme.enabledHint()
                } else {
                    stringResource(R.string.settings_disabled)
                },
                enabled = state.seasonalDecorationsEnabled,
                onClick = { onEvent(SettingsState.Event.SeasonalDecorationsToggled) },
            )
        }
        SettingsMobileSection(title = stringResource(R.string.settings_section_posters)) {
            SettingsMobileIconChoiceRow(
                label = stringResource(R.string.settings_mobile_poster_size),
                hint = state.posterCardSize.hint(),
                choices = PosterCardSize.entries.map { SettingsMobileIconChoice(it, it.icon(), it.label()) },
                selected = state.posterCardSize,
                onSelected = { onEvent(SettingsState.Event.PosterCardSizeSelected(it)) },
            )
            SettingsMobileIconChoiceRow(
                label = stringResource(R.string.settings_title_card_layout_title),
                hint = state.titleCardLayout.hint(),
                choices = TitleCardLayout.entries.map { SettingsMobileIconChoice(it, it.icon(), it.label()) },
                selected = state.titleCardLayout,
                onSelected = { onEvent(SettingsState.Event.TitleCardLayoutSelected(it)) },
            )
            SettingsMobileOptionRow(
                label = stringResource(R.string.settings_poster_quality_title),
                value = state.posterQuality.label(),
                hint = state.posterQuality.hint(),
                onClick = { onPickerRequested(SettingsMobilePicker.POSTER_QUALITY) },
            )
            SettingsMobileToggleRow(
                label = stringResource(R.string.settings_show_catalog_title_year),
                hint = if (state.showCatalogTitleYear) {
                    stringResource(R.string.settings_show_catalog_title_year_enabled)
                } else {
                    stringResource(R.string.settings_disabled)
                },
                enabled = state.showCatalogTitleYear,
                onClick = { onEvent(SettingsState.Event.ShowCatalogTitleYearToggled) },
            )
            SettingsMobileToggleRow(
                label = stringResource(R.string.settings_show_episode_flags),
                hint = if (state.showEpisodeFlags) {
                    stringResource(R.string.settings_show_episode_flags_enabled)
                } else {
                    stringResource(R.string.settings_disabled)
                },
                enabled = state.showEpisodeFlags,
                onClick = { onEvent(SettingsState.Event.ShowEpisodeFlagsToggled) },
            )
            SettingsMobileToggleRow(
                label = stringResource(R.string.settings_show_library_title_year),
                hint = if (state.showLibraryTitleYear) {
                    stringResource(R.string.settings_show_library_title_year_enabled)
                } else {
                    stringResource(R.string.settings_disabled)
                },
                enabled = state.showLibraryTitleYear,
                onClick = { onEvent(SettingsState.Event.ShowLibraryTitleYearToggled) },
            )
        }
        SettingsMobileSection(title = stringResource(R.string.settings_mobile_section_library)) {
            SettingsMobileIconChoiceRow(
                label = stringResource(R.string.settings_library_continue_watching_card_size_title),
                hint = state.libraryContinueWatchingCardSize.hint(),
                choices = LibraryContinueWatchingCardSize.entries.map {
                    SettingsMobileIconChoice(it, it.icon(), it.label())
                },
                selected = state.libraryContinueWatchingCardSize,
                onSelected = { onEvent(SettingsState.Event.LibraryContinueWatchingCardSizeSelected(it)) },
            )
            SettingsMobileNavigationRow(
                label = stringResource(R.string.settings_library_tabs_order),
                hint = stringResource(R.string.settings_library_tabs_order_hint),
                onClick = { onEvent(SettingsState.Event.LibraryTabOrderSelected) },
            )
        }
        SettingsMobileSection(title = stringResource(R.string.settings_home_sections_title)) {
            androidx.compose.material3.Text(
                text = stringResource(R.string.settings_home_sections_hint),
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = androidx.compose.ui.Modifier.padding(horizontal = 16.dp),
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
            SettingsMobileActionRow(
                label = stringResource(R.string.settings_home_sections_reset),
                hint = stringResource(R.string.settings_home_sections_reset_hint),
                enabled = true,
                onClick = { onEvent(SettingsState.Event.HomeSectionOrderReset) },
            )
        }
        SettingsMobileSection(title = stringResource(R.string.settings_section_new_episodes)) {
            // Список источников без включённого блока ни на что не влияет.
            if (state.newEpisodesSectionEnabled) {
                SettingsMobileOptionRow(
                    label = stringResource(R.string.settings_new_episodes_sources_title),
                    value = state.newEpisodesSources.newEpisodesSourcesValue(),
                    onClick = { onPickerRequested(SettingsMobilePicker.NEW_EPISODES_SOURCES) },
                )
            }
        }
    }
}

private fun HomeSectionKind.labelRes(): Int = when (this) {
    HomeSectionKind.MY_NEW_EPISODES -> R.string.settings_home_section_my_new_episodes
    HomeSectionKind.NEW_RELEASES -> R.string.settings_home_section_new_releases
    HomeSectionKind.NEW_TITLES -> R.string.settings_home_section_new_titles
    HomeSectionKind.RECOMMENDATIONS -> R.string.settings_home_section_recommendations
    HomeSectionKind.ANNOUNCEMENTS -> R.string.settings_home_section_announcements
    HomeSectionKind.COLLECTIONS -> R.string.settings_home_section_collections
}
