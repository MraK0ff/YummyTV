package su.afk.yummy.tv.feature.settings.view.category

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.model.settings.LibraryContinueWatchingCardSize
import su.afk.yummy.tv.core.model.settings.PosterCardSize
import su.afk.yummy.tv.core.model.settings.TitleCardLayout
import su.afk.yummy.tv.feature.settings.HomeSectionKind
import su.afk.yummy.tv.feature.settings.SettingsState
import su.afk.yummy.tv.feature.settings.model.DetailsButtonMoveDirection
import su.afk.yummy.tv.feature.settings.common.utils.hint
import su.afk.yummy.tv.feature.settings.common.utils.icon
import su.afk.yummy.tv.feature.settings.common.utils.label
import su.afk.yummy.tv.feature.settings.model.SettingsTvPicker
import su.afk.yummy.tv.feature.settings.common.utils.enabledHint
import su.afk.yummy.tv.feature.settings.presentation.R
import su.afk.yummy.tv.feature.settings.utils.hint
import su.afk.yummy.tv.feature.settings.utils.restoreCategoryFocusOnLeft
import su.afk.yummy.tv.feature.settings.view.DetailsButtonOrderResetRow
import su.afk.yummy.tv.feature.settings.view.DetailsButtonOrderRow
import su.afk.yummy.tv.feature.settings.view.SettingsBlockGap
import su.afk.yummy.tv.feature.settings.view.SettingsDivider
import su.afk.yummy.tv.feature.settings.view.SettingsSectionTitle
import su.afk.yummy.tv.feature.settings.view.SettingsTvIconChoice
import su.afk.yummy.tv.feature.settings.view.SettingsTvIconChoiceRow
import su.afk.yummy.tv.feature.settings.view.ToggleRow

@Composable
internal fun SettingsTvAppearanceContent(
    state: SettingsState.State,
    tabFocusRequester: FocusRequester,
    tabContentFocusRequester: FocusRequester,
    onEvent: (SettingsState.Event) -> Unit,
    pickerRow: SettingsTvPickerRow,
) {
    SettingsSectionTitle(text = stringResource(R.string.settings_section_general_appearance))
    pickerRow(
        SettingsTvPicker.THEME,
        Modifier
            .focusRequester(tabContentFocusRequester)
            .restoreCategoryFocusOnLeft(tabFocusRequester),
    )
    SettingsDivider()
    pickerRow(SettingsTvPicker.BACKGROUND, Modifier)
    SettingsDivider()
    pickerRow(SettingsTvPicker.NAV_TRANSITION, Modifier)
    SettingsDivider()
    ToggleRow(
        label = stringResource(R.string.settings_seasonal_decorations),
        hint = if (state.seasonalDecorationsEnabled) {
            state.seasonalTheme.enabledHint()
        } else {
            stringResource(R.string.settings_disabled)
        },
        enabled = state.seasonalDecorationsEnabled,
        onClick = { onEvent(SettingsState.Event.SeasonalDecorationsToggled) },
        modifier = Modifier.restoreCategoryFocusOnLeft(tabFocusRequester),
    )
    SettingsBlockGap()
    SettingsSectionTitle(text = stringResource(R.string.settings_section_posters))
    SettingsTvIconChoiceRow(
        label = stringResource(R.string.settings_poster_size_title),
        hint = state.posterCardSize.hint(),
        choices = PosterCardSize.entries.map { SettingsTvIconChoice(it, it.icon(), it.label()) },
        selected = state.posterCardSize,
        onSelected = { onEvent(SettingsState.Event.PosterCardSizeSelected(it)) },
        firstChoiceModifier = Modifier.restoreCategoryFocusOnLeft(tabFocusRequester),
    )
    SettingsDivider()
    SettingsTvIconChoiceRow(
        label = stringResource(R.string.settings_title_card_layout_title),
        hint = state.titleCardLayout.hint(),
        choices = TitleCardLayout.entries.map { SettingsTvIconChoice(it, it.icon(), it.label()) },
        selected = state.titleCardLayout,
        onSelected = { onEvent(SettingsState.Event.TitleCardLayoutSelected(it)) },
        firstChoiceModifier = Modifier.restoreCategoryFocusOnLeft(tabFocusRequester),
    )
    SettingsDivider()
    pickerRow(SettingsTvPicker.POSTER_QUALITY, Modifier)
    SettingsDivider()
    ToggleRow(
        label = stringResource(R.string.settings_show_catalog_title_year),
        hint = if (state.showCatalogTitleYear) {
            stringResource(R.string.settings_show_catalog_title_year_enabled)
        } else {
            stringResource(R.string.settings_disabled)
        },
        enabled = state.showCatalogTitleYear,
        onClick = { onEvent(SettingsState.Event.ShowCatalogTitleYearToggled) },
        modifier = Modifier.restoreCategoryFocusOnLeft(tabFocusRequester),
    )
    SettingsDivider()
    ToggleRow(
        label = stringResource(R.string.settings_show_episode_flags),
        hint = if (state.showEpisodeFlags) {
            stringResource(R.string.settings_show_episode_flags_enabled)
        } else {
            stringResource(R.string.settings_disabled)
        },
        enabled = state.showEpisodeFlags,
        onClick = { onEvent(SettingsState.Event.ShowEpisodeFlagsToggled) },
        modifier = Modifier.restoreCategoryFocusOnLeft(tabFocusRequester),
    )
    SettingsDivider()
    ToggleRow(
        label = stringResource(R.string.settings_show_library_title_year),
        hint = if (state.showLibraryTitleYear) {
            stringResource(R.string.settings_show_library_title_year_enabled)
        } else {
            stringResource(R.string.settings_disabled)
        },
        enabled = state.showLibraryTitleYear,
        onClick = { onEvent(SettingsState.Event.ShowLibraryTitleYearToggled) },
        modifier = Modifier.restoreCategoryFocusOnLeft(tabFocusRequester),
    )
    SettingsBlockGap()
    SettingsSectionTitle(text = stringResource(R.string.settings_home_sections_title))
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
    HomeSectionKind.entries.forEach { section ->
        val enabled = if (section == HomeSectionKind.MY_NEW_EPISODES) {
            state.newEpisodesSectionEnabled
        } else {
            section !in state.hiddenHomeSections
        }
        ToggleRow(
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
            modifier = Modifier.restoreCategoryFocusOnLeft(tabFocusRequester),
        )
        SettingsDivider()
    }
    DetailsButtonOrderResetRow(
        label = stringResource(R.string.settings_home_sections_reset),
        hint = stringResource(R.string.settings_home_sections_reset_hint),
        onReset = { onEvent(SettingsState.Event.HomeSectionOrderReset) },
        modifier = Modifier.restoreCategoryFocusOnLeft(tabFocusRequester),
    )
    SettingsBlockGap()
    SettingsSectionTitle(text = stringResource(R.string.settings_mobile_section_library))
    SettingsTvIconChoiceRow(
        label = stringResource(R.string.settings_library_continue_watching_card_size_title),
        hint = state.libraryContinueWatchingCardSize.hint(),
        choices = LibraryContinueWatchingCardSize.entries.map {
            SettingsTvIconChoice(it, it.icon(), it.label())
        },
        selected = state.libraryContinueWatchingCardSize,
        onSelected = { onEvent(SettingsState.Event.LibraryContinueWatchingCardSizeSelected(it)) },
        firstChoiceModifier = Modifier.restoreCategoryFocusOnLeft(tabFocusRequester),
    )
    SettingsDivider()
    pickerRow(SettingsTvPicker.LIBRARY_TAB_ORDER, Modifier)
    SettingsBlockGap()
    SettingsSectionTitle(text = stringResource(R.string.settings_section_new_episodes))
    if (state.newEpisodesSectionEnabled) {
        pickerRow(SettingsTvPicker.NEW_EPISODES_SOURCES, Modifier.restoreCategoryFocusOnLeft(tabFocusRequester))
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
