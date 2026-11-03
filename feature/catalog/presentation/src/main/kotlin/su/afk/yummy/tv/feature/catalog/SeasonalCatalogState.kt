package su.afk.yummy.tv.feature.catalog

import su.afk.yummy.tv.core.mvi.UiEffect
import su.afk.yummy.tv.core.mvi.UiEvent
import su.afk.yummy.tv.core.mvi.UiState
import su.afk.yummy.tv.core.model.text.UiText
import su.afk.yummy.tv.domain.home.model.HomeFeed
import su.afk.yummy.tv.domain.home.model.HomeFeedItemAction

class SeasonalCatalogState {
    data class State(
        val feed: HomeFeed? = null,
        val isLoading: Boolean = true,
        val error: UiText? = null,
    ) : UiState

    sealed interface Event : UiEvent {
        data object RetrySelected : Event
        data class ItemSelected(val action: HomeFeedItemAction) : Event
    }

    sealed interface Effect : UiEffect
}
