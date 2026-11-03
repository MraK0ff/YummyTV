package su.afk.yummy.tv.feature.catalog

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.common.coroutines.runSuspendCatching
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.domain.home.model.HomeFeedItemAction
import su.afk.yummy.tv.domain.home.usecase.GetCachedHomeFeedUseCase
import su.afk.yummy.tv.domain.home.usecase.GetHomeFeedUseCase
import su.afk.yummy.tv.feature.bloggers.IBloggerVideosNavigator
import su.afk.yummy.tv.feature.collection.ICollectionNavigator
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import javax.inject.Inject

@HiltViewModel
class SeasonalCatalogViewModel @Inject constructor(
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val nav: INavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val collectionNavigator: ICollectionNavigator,
    private val bloggerVideosNavigator: IBloggerVideosNavigator,
    private val getHomeFeed: GetHomeFeedUseCase,
    private val getCachedHomeFeed: GetCachedHomeFeedUseCase,
) : BaseViewModel<SeasonalCatalogState.State, SeasonalCatalogState.Event, SeasonalCatalogState.Effect>() {

    override fun createInitialState() = SeasonalCatalogState.State()

    init {
        load()
    }

    override fun onEvent(event: SeasonalCatalogState.Event) {
        when (event) {
            SeasonalCatalogState.Event.RetrySelected -> load()
            is SeasonalCatalogState.Event.ItemSelected -> openItem(event.action)
        }
    }

    override fun onRetry() {
        load()
    }

    private fun openItem(action: HomeFeedItemAction) {
        val destination = when (action) {
            is HomeFeedItemAction.OpenSeries -> detailsNavigator.getDetailsDest(action.seriesId)
            is HomeFeedItemAction.OpenCollection -> collectionNavigator.getCollectionDest(action.collectionId)
            is HomeFeedItemAction.OpenVideo -> bloggerVideosNavigator.video(action.videoId)
        }
        nav.navigate(destination)
    }

    private fun load() {
        viewModelScope.launch {
            val cached = runSuspendCatching { getCachedHomeFeed() }.getOrNull()
            if (cached != null) {
                setState { copy(feed = cached, isLoading = false, error = null) }
            } else {
                setState { copy(isLoading = true, error = null) }
            }

            runSuspendCatching { getHomeFeed() }.fold(
                onSuccess = { feed ->
                    setState { copy(feed = feed, isLoading = false, error = null) }
                },
                onFailure = { error ->
                    setState {
                        copy(
                            isLoading = false,
                            error = if (feed == null) {
                                su.afk.yummy.tv.core.model.text.UiText.Failure(error)
                            } else {
                                this.error
                            },
                        )
                    }
                },
            )
        }
    }
}
