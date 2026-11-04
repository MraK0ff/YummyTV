package su.afk.yummy.tv.feature.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.domain.catalog.updates.UpdatesFeedItem
import su.afk.yummy.tv.domain.catalog.updates.UpdatesFeedRepository
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import javax.inject.Inject

data class UpdatesFeedUiState(
    val items: List<UpdatesFeedItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class UpdatesFeedViewModel @Inject constructor(
    private val repository: UpdatesFeedRepository,
    private val navigation: INavigationManager,
    private val detailsNavigator: IDetailsNavigator,
) : ViewModel() {
    private val _state = MutableStateFlow(UpdatesFeedUiState())
    val state = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        if (_state.value.isLoading) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                _state.update { it.copy(items = repository.load(), isLoading = false) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _state.update {
                    it.copy(isLoading = false, errorMessage = error.message ?: "Не удалось загрузить обновления")
                }
            }
        }
    }

    fun openAnime(animeId: Int) {
        navigation.navigate(detailsNavigator.getDetailsDest(animeId))
    }
}
