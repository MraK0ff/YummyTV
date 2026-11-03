package su.afk.yummy.tv.feature.playersetup.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HighQuality
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Timer
import su.afk.yummy.tv.core.model.settings.AppTheme
import su.afk.yummy.tv.core.model.settings.BackgroundStyle
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.mobile.layout.mobileContentMaxWidth
import su.afk.yummy.tv.core.model.settings.PlayerOrientationMode
import su.afk.yummy.tv.core.model.settings.PreferredVideoQuality
import su.afk.yummy.tv.core.model.settings.YaniContentLanguage
import su.afk.yummy.tv.feature.playersetup.PlayerSetupState
import su.afk.yummy.tv.feature.playersetup.mobile.model.PlayerSetupMobilePicker
import su.afk.yummy.tv.feature.playersetup.mobile.model.PlayerSetupMobilePickerOption
import su.afk.yummy.tv.feature.playersetup.mobile.utils.setupHint
import su.afk.yummy.tv.feature.playersetup.mobile.utils.setupIcon
import su.afk.yummy.tv.feature.playersetup.mobile.utils.setupLabel
import su.afk.yummy.tv.feature.playersetup.mobile.view.PlayerSetupMobileOptionRow
import su.afk.yummy.tv.feature.playersetup.mobile.view.PlayerSetupMobilePickerSheet
import su.afk.yummy.tv.feature.playersetup.mobile.view.PlayerSetupMobileSection
import su.afk.yummy.tv.feature.playersetup.mobile.view.PlayerSetupMobileToggleRow
import su.afk.yummy.tv.feature.playersetup.presentation.R
import su.afk.yummy.tv.core.designsystem.R as CoreR

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PlayerSetupMobileScreen(
    state: PlayerSetupState.State,
    effect: Flow<PlayerSetupState.Effect>,
    onEvent: (PlayerSetupState.Event) -> Unit,
) {
    var activePicker by remember { mutableStateOf<PlayerSetupMobilePicker?>(null) }

    BaseScreen(isScroll = false) {
        Column(
            modifier = Modifier
                .mobileContentMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.player_setup_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.player_setup_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            PlayerSetupMobileSection(title = stringResource(R.string.player_setup_mobile_section_general)) {
                PlayerSetupMobileOptionRow(
                    label = stringResource(R.string.player_setup_language_label),
                    icon = Icons.Outlined.Language,
                    value = state.contentLanguage.setupLabel(),
                    hint = stringResource(R.string.player_setup_language_hint),
                    onClick = { activePicker = PlayerSetupMobilePicker.LANGUAGE },
                )
            }

            PlayerSetupMobileSection(title = stringResource(R.string.player_setup_mobile_section_timings)) {
                PlayerSetupMobileToggleRow(
                    label = stringResource(R.string.player_setup_opening_timeline_label),
                    icon = Icons.Outlined.Timeline,
                    hint = stringResource(R.string.player_setup_opening_timeline_hint),
                    enabled = state.showOpeningOnTimeline,
                    onClick = { onEvent(PlayerSetupState.Event.ShowOpeningOnTimelineToggled) },
                )
                PlayerSetupMobileToggleRow(
                    label = stringResource(R.string.player_setup_auto_skip_label),
                    icon = Icons.Outlined.SkipNext,
                    hint = stringResource(R.string.player_setup_auto_skip_hint),
                    enabled = state.autoSkipOpeningsEndings,
                    onClick = { onEvent(PlayerSetupState.Event.AutoSkipOpeningsEndingsToggled) },
                )
            }

            PlayerSetupMobileSection(title = stringResource(R.string.player_setup_mobile_section_episodes)) {
                PlayerSetupMobileToggleRow(
                    label = stringResource(R.string.player_setup_auto_play_label),
                    icon = Icons.Outlined.PlayCircle,
                    hint = stringResource(R.string.player_setup_auto_play_hint),
                    enabled = state.autoPlayNextEpisode,
                    onClick = { onEvent(PlayerSetupState.Event.AutoPlayNextEpisodeToggled) },
                )
                PlayerSetupMobileToggleRow(
                    label = stringResource(R.string.player_setup_suggest_next_label),
                    icon = Icons.Outlined.Lightbulb,
                    hint = stringResource(R.string.player_setup_suggest_next_hint),
                    enabled = state.suggestNextEpisodeOnWatched,
                    onClick = { onEvent(PlayerSetupState.Event.SuggestNextEpisodeOnWatchedToggled) },
                )
            }

            PlayerSetupMobileSection(title = stringResource(R.string.player_setup_mobile_section_playback)) {
                PlayerSetupMobileOptionRow(
                    label = stringResource(R.string.player_setup_mobile_orientation_label),
                    icon = Icons.Outlined.ScreenRotation,
                    value = state.playerOrientationMode.setupLabel(),
                    valueIcon = state.playerOrientationMode.setupIcon(),
                    hint = stringResource(R.string.player_setup_mobile_orientation_hint),
                    onClick = { activePicker = PlayerSetupMobilePicker.ORIENTATION },
                )
                PlayerSetupMobileToggleRow(
                    label = stringResource(R.string.player_setup_mobile_pip_label),
                    icon = Icons.Outlined.PictureInPictureAlt,
                    hint = stringResource(R.string.player_setup_mobile_pip_hint),
                    enabled = state.pictureInPictureEnabled,
                    onClick = { onEvent(PlayerSetupState.Event.PictureInPictureToggled) },
                )
                PlayerSetupMobileToggleRow(
                    label = stringResource(R.string.player_setup_adaptive_quality_label),
                    icon = Icons.Outlined.Speed,
                    hint = stringResource(R.string.player_setup_adaptive_quality_hint),
                    enabled = state.adaptiveVideoQualityEnabled,
                    onClick = { onEvent(PlayerSetupState.Event.AdaptiveVideoQualityToggled) },
                )
                PlayerSetupMobileOptionRow(
                    label = stringResource(R.string.player_setup_quality_label),
                    icon = Icons.Outlined.HighQuality,
                    value = state.preferredVideoQuality.setupLabel(),
                    hint = stringResource(R.string.player_setup_quality_hint),
                    onClick = { activePicker = PlayerSetupMobilePicker.QUALITY },
                )
            }

            PlayerSetupMobileSection(title = stringResource(R.string.player_setup_mobile_section_playback)) {
                PlayerSetupMobileOptionRow(
                    label = stringResource(R.string.player_setup_auto_skip_delay_label),
                    icon = Icons.Outlined.Timer,
                    value = if (state.autoSkipDelaySeconds == 0) stringResource(R.string.player_setup_instant) else stringResource(R.string.player_setup_seconds, state.autoSkipDelaySeconds),
                    hint = stringResource(R.string.player_setup_auto_skip_delay_hint),
                    onClick = { activePicker = PlayerSetupMobilePicker.AUTO_SKIP_DELAY },
                    enabled = state.autoSkipOpeningsEndings,
                )
                PlayerSetupMobileOptionRow(
                    label = stringResource(R.string.player_setup_autoplay_delay_label),
                    icon = Icons.Outlined.Timer,
                    value = if (state.nextEpisodeSwitchDelaySeconds == 0) stringResource(R.string.player_setup_instant) else stringResource(R.string.player_setup_seconds, state.nextEpisodeSwitchDelaySeconds),
                    hint = stringResource(R.string.player_setup_autoplay_delay_hint),
                    onClick = { activePicker = PlayerSetupMobilePicker.AUTOPLAY_DELAY },
                    enabled = state.autoPlayNextEpisode,
                )
                PlayerSetupMobileOptionRow(
                    label = stringResource(R.string.player_setup_controls_timeout_label),
                    icon = Icons.Outlined.Timer,
                    value = stringResource(R.string.player_setup_seconds, state.playerControlsAutoHideSeconds),
                    hint = stringResource(R.string.player_setup_controls_timeout_hint),
                    onClick = { activePicker = PlayerSetupMobilePicker.CONTROLS_TIMEOUT },
                )
            }
            PlayerSetupMobileSection(title = stringResource(R.string.player_setup_mobile_section_appearance)) {
                PlayerSetupMobileOptionRow(
                    label = stringResource(R.string.player_setup_theme_label),
                    icon = Icons.Outlined.Palette,
                    value = state.appTheme.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() },
                    hint = stringResource(R.string.player_setup_theme_hint),
                    onClick = { activePicker = PlayerSetupMobilePicker.THEME },
                )
                PlayerSetupMobileOptionRow(
                    label = stringResource(R.string.player_setup_background_label),
                    icon = Icons.Outlined.DarkMode,
                    value = state.backgroundStyle.name.lowercase().replaceFirstChar { it.uppercase() },
                    hint = stringResource(R.string.player_setup_background_hint),
                    onClick = { activePicker = PlayerSetupMobilePicker.BACKGROUND },
                )
            }

            Button(
                onClick = { onEvent(PlayerSetupState.Event.DoneSelected) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(CoreR.string.common_done))
            }
            TextButton(
                onClick = { onEvent(PlayerSetupState.Event.MoreSettingsSelected) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.player_setup_mobile_more_settings))
            }
        }
    }

    when (activePicker) {
        PlayerSetupMobilePicker.LANGUAGE -> PlayerSetupMobilePickerSheet(
            title = stringResource(R.string.player_setup_language_label),
            selectedValue = state.contentLanguage,
            options = YaniContentLanguage.entries.map {
                PlayerSetupMobilePickerOption(it, it.setupLabel())
            },
            onDismiss = { activePicker = null },
            onSelected = {
                onEvent(PlayerSetupState.Event.ContentLanguageSelected(it))
                activePicker = null
            },
        )

        PlayerSetupMobilePicker.ORIENTATION -> PlayerSetupMobilePickerSheet(
            title = stringResource(R.string.player_setup_mobile_orientation_label),
            selectedValue = state.playerOrientationMode,
            options = PlayerOrientationMode.entries.map {
                PlayerSetupMobilePickerOption(it, it.setupLabel(), it.setupHint(), it.setupIcon())
            },
            onDismiss = { activePicker = null },
            onSelected = {
                onEvent(PlayerSetupState.Event.PlayerOrientationModeSelected(it))
                activePicker = null
            },
        )

        PlayerSetupMobilePicker.QUALITY -> PlayerSetupMobilePickerSheet(
            title = stringResource(R.string.player_setup_quality_label),
            selectedValue = state.preferredVideoQuality,
            options = PreferredVideoQuality.entries.map {
                PlayerSetupMobilePickerOption(it, it.setupLabel(), it.setupHint())
            },
            onDismiss = { activePicker = null },
            onSelected = {
                onEvent(PlayerSetupState.Event.PreferredVideoQualitySelected(it))
                activePicker = null
            },
        )


        PlayerSetupMobilePicker.AUTO_SKIP_DELAY -> PlayerSetupMobilePickerSheet(
            title = stringResource(R.string.player_setup_auto_skip_delay_label),
            selectedValue = state.autoSkipDelaySeconds,
            options = (0..15).map { PlayerSetupMobilePickerOption(it, if (it == 0) "Instant" else "$it seconds") },
            onDismiss = { activePicker = null },
            onSelected = { onEvent(PlayerSetupState.Event.AutoSkipDelaySelected(it)); activePicker = null },
        )
        PlayerSetupMobilePicker.AUTOPLAY_DELAY -> PlayerSetupMobilePickerSheet(
            title = stringResource(R.string.player_setup_autoplay_delay_label),
            selectedValue = state.nextEpisodeSwitchDelaySeconds,
            options = (0..30).map { PlayerSetupMobilePickerOption(it, if (it == 0) "Instant" else "$it seconds") },
            onDismiss = { activePicker = null },
            onSelected = { onEvent(PlayerSetupState.Event.NextEpisodeDelaySelected(it)); activePicker = null },
        )
        PlayerSetupMobilePicker.CONTROLS_TIMEOUT -> PlayerSetupMobilePickerSheet(
            title = stringResource(R.string.player_setup_controls_timeout_label),
            selectedValue = state.playerControlsAutoHideSeconds,
            options = (1..10).map { PlayerSetupMobilePickerOption(it, "$it seconds") },
            onDismiss = { activePicker = null },
            onSelected = { onEvent(PlayerSetupState.Event.ControlsTimeoutSelected(it)); activePicker = null },
        )
        PlayerSetupMobilePicker.THEME -> PlayerSetupMobilePickerSheet(
            title = stringResource(R.string.player_setup_theme_label),
            selectedValue = state.appTheme,
            options = AppTheme.entries.map { PlayerSetupMobilePickerOption(it, it.name.replace('_', ' ').lowercase().replaceFirstChar { c -> c.uppercase() }) },
            onDismiss = { activePicker = null },
            onSelected = { onEvent(PlayerSetupState.Event.ThemeSelected(it)); activePicker = null },
        )
        PlayerSetupMobilePicker.BACKGROUND -> PlayerSetupMobilePickerSheet(
            title = stringResource(R.string.player_setup_background_label),
            selectedValue = state.backgroundStyle,
            options = BackgroundStyle.entries.map { PlayerSetupMobilePickerOption(it, it.name.lowercase().replaceFirstChar { c -> c.uppercase() }) },
            onDismiss = { activePicker = null },
            onSelected = { onEvent(PlayerSetupState.Event.BackgroundSelected(it)); activePicker = null },
        )

        null -> Unit
    }
}
