package su.afk.yummy.tv.feature.playersetup

import androidx.compose.runtime.Immutable
import su.afk.yummy.tv.core.model.settings.AppTheme
import su.afk.yummy.tv.core.model.settings.BackgroundStyle
import su.afk.yummy.tv.core.model.settings.PlayerOrientationMode
import su.afk.yummy.tv.core.model.settings.PreferredVideoQuality
import su.afk.yummy.tv.core.model.settings.YaniContentLanguage
import su.afk.yummy.tv.core.mvi.UiEffect
import su.afk.yummy.tv.core.mvi.UiEvent
import su.afk.yummy.tv.core.mvi.UiState

class PlayerSetupState {

    @Immutable
    data class State(
        val showOpeningOnTimeline: Boolean = false,
        val autoSkipOpeningsEndings: Boolean = false,
        val autoPlayNextEpisode: Boolean = false,
        val suggestNextEpisodeOnWatched: Boolean = true,
        val adaptiveVideoQualityEnabled: Boolean = true,
        val preferredVideoQuality: PreferredVideoQuality = PreferredVideoQuality.BEST,
        val playerOrientationMode: PlayerOrientationMode = PlayerOrientationMode.SYSTEM,
        val pictureInPictureEnabled: Boolean = true,
        val contentLanguage: YaniContentLanguage = YaniContentLanguage.DEFAULT,
        val autoSkipDelaySeconds: Int = 0,
        val nextEpisodeSwitchDelaySeconds: Int = 0,
        val playerControlsAutoHideSeconds: Int = 2,
        val appTheme: AppTheme = AppTheme.GRAPHITE,
        val backgroundStyle: BackgroundStyle = BackgroundStyle.AMOLED,
    ) : UiState

    sealed interface Event : UiEvent {
        data object ShowOpeningOnTimelineToggled : Event
        data object AutoSkipOpeningsEndingsToggled : Event
        data object AutoPlayNextEpisodeToggled : Event
        data class AutoSkipDelaySelected(val seconds: Int) : Event
        data class NextEpisodeDelaySelected(val seconds: Int) : Event
        data class ControlsTimeoutSelected(val seconds: Int) : Event
        data class ThemeSelected(val theme: AppTheme) : Event
        data class BackgroundSelected(val style: BackgroundStyle) : Event
        data object SuggestNextEpisodeOnWatchedToggled : Event
        data object PictureInPictureToggled : Event
        data object AdaptiveVideoQualityToggled : Event
        data class ContentLanguageSelected(val language: YaniContentLanguage) : Event
        data class PreferredVideoQualitySelected(val quality: PreferredVideoQuality) : Event
        data class PlayerOrientationModeSelected(val mode: PlayerOrientationMode) : Event
        data object DoneSelected : Event

        /** Пользователь выбрал «Остальное в настройках» — закрываем экран и открываем настройки. */
        data object MoreSettingsSelected : Event
    }

    sealed interface Effect : UiEffect
}
