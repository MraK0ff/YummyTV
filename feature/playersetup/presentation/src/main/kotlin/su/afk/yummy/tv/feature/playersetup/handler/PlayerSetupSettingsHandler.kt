package su.afk.yummy.tv.feature.playersetup.handler

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import su.afk.yummy.tv.core.model.settings.ImageFormatSupport
import su.afk.yummy.tv.core.model.settings.PlayerOrientationMode
import su.afk.yummy.tv.core.model.settings.PreferredVideoQuality
import su.afk.yummy.tv.core.model.settings.YaniContentLanguage
import su.afk.yummy.tv.core.model.settings.choosePosterQuality
import su.afk.yummy.tv.core.preferences.api.YaniAccountSettingsStore
import su.afk.yummy.tv.core.preferences.settings.AppearanceSettingsStore
import su.afk.yummy.tv.core.preferences.settings.PlayerSettingsStore
import su.afk.yummy.tv.feature.playersetup.PlayerSetupState
import javax.inject.Inject

/** Читает и записывает настройки плеера, которые предлагаются на первичной настройке. */
internal class PlayerSetupSettingsHandler @Inject constructor(
    private val store: PlayerSettingsStore,
    private val accountStore: YaniAccountSettingsStore,
    private val appearanceStore: AppearanceSettingsStore,
    private val imageFormatSupport: ImageFormatSupport,
) {

    /**
     * Проверяет, что устройство реально декодирует AVIF/WebP, и сохраняет подходящее качество
     * постеров. Если качество уже сохранено (выбор пользователя), ничего не меняет.
     */
    suspend fun detectPosterQualityIfNeeded() {
        if (appearanceStore.isPosterQualityStored()) return
        val quality = choosePosterQuality(
            avifSupported = imageFormatSupport.isAvifSupported(),
            webpSupported = imageFormatSupport.isWebpSupported(),
        )
        appearanceStore.initPosterQualityIfAbsent(quality)
    }

    fun observe(): Flow<PlayerSetupState.State> = combine(
        combine(
            store.showOpeningOnTimeline,
            store.autoSkipOpeningsEndings,
            store.autoPlayNextEpisode,
            store.suggestNextEpisodeOnWatched,
        ) { showOpening, autoSkip, autoPlay, suggestNext ->
            PlayerSetupState.State(
                showOpeningOnTimeline = showOpening,
                autoSkipOpeningsEndings = autoSkip,
                autoPlayNextEpisode = autoPlay,
                suggestNextEpisodeOnWatched = suggestNext,
            )
        },
        combine(
            store.autoSkipDelaySeconds,
            store.nextEpisodeSwitchDelaySeconds,
            store.playerControlsAutoHideSeconds,
        ) { skip, next, controls -> Triple(skip, next, controls) },
        combine(
            store.adaptiveVideoQualityEnabled,
            store.preferredVideoQuality,
            store.playerOrientationMode,
            store.pictureInPictureEnabled,
        ) { adaptive, quality, orientation, pip ->
            PlayerSetupState.State(
                adaptiveVideoQualityEnabled = adaptive,
                preferredVideoQuality = quality,
                playerOrientationMode = orientation,
                pictureInPictureEnabled = pip,
            )
        },
        combine(
            accountStore.yaniContentLanguage,
            appearanceStore.appTheme,
            appearanceStore.backgroundStyle,
        ) { language, theme, background ->
            Triple(language, theme, background)
        },
    ) { toggles, delays, playback, appearance ->
        toggles.copy(
            autoSkipDelaySeconds = delays.first,
            nextEpisodeSwitchDelaySeconds = delays.second,
            playerControlsAutoHideSeconds = delays.third,
            adaptiveVideoQualityEnabled = playback.adaptiveVideoQualityEnabled,
            preferredVideoQuality = playback.preferredVideoQuality,
            playerOrientationMode = playback.playerOrientationMode,
            pictureInPictureEnabled = playback.pictureInPictureEnabled,
            contentLanguage = appearance.first,
            appTheme = appearance.second,
            backgroundStyle = appearance.third,
        )
    }

    suspend fun setShowOpeningOnTimeline(enabled: Boolean) = store.setShowOpeningOnTimeline(enabled)

    suspend fun setAutoSkipOpeningsEndings(enabled: Boolean) = store.setAutoSkipOpeningsEndings(enabled)

    suspend fun setAutoPlayNextEpisode(enabled: Boolean) = store.setAutoPlayNextEpisode(enabled)
    suspend fun setAutoSkipDelaySeconds(seconds: Int) = store.setAutoSkipDelaySeconds(seconds)
    suspend fun setNextEpisodeSwitchDelaySeconds(seconds: Int) = store.setNextEpisodeSwitchDelaySeconds(seconds)
    suspend fun setPlayerControlsAutoHideSeconds(seconds: Int) = store.setPlayerControlsAutoHideSeconds(seconds)
    suspend fun setTheme(theme: su.afk.yummy.tv.core.model.settings.AppTheme) = appearanceStore.setAppTheme(theme)
    suspend fun setBackground(style: su.afk.yummy.tv.core.model.settings.BackgroundStyle) = appearanceStore.setBackgroundStyle(style)

    suspend fun setSuggestNextEpisodeOnWatched(enabled: Boolean) =
        store.setSuggestNextEpisodeOnWatched(enabled)


    suspend fun setPictureInPictureEnabled(enabled: Boolean) = store.setPictureInPictureEnabled(enabled)

    suspend fun setContentLanguage(language: YaniContentLanguage) =
        accountStore.setYaniContentLanguage(language)

    suspend fun setAdaptiveVideoQualityEnabled(enabled: Boolean) =
        store.setAdaptiveVideoQualityEnabled(enabled)

    suspend fun setPreferredVideoQuality(quality: PreferredVideoQuality) =
        store.setPreferredVideoQuality(quality)

    suspend fun setPlayerOrientationMode(mode: PlayerOrientationMode) =
        store.setPlayerOrientationMode(mode)
}
