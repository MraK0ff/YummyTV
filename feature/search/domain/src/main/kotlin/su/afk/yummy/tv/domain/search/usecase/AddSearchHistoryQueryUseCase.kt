package su.afk.yummy.tv.domain.search.usecase

import su.afk.yummy.tv.domain.search.model.withSearchQuery
import su.afk.yummy.tv.domain.search.repository.SearchHistoryRepository
import javax.inject.Inject

/**
 * Запоминает поисковый запрос в истории: поднимает его наверх без дублей и держит только
 * последние запросы. Пустой запрос не сохраняется.
 */
class AddSearchHistoryQueryUseCase @Inject constructor(
    private val repository: SearchHistoryRepository,
) {
    suspend operator fun invoke(query: String) {
        if (query.isBlank()) return
        repository.update { history -> history.withSearchQuery(query) }
    }
}
