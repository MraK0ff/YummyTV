package su.afk.yummy.tv.domain.search.usecase

import su.afk.yummy.tv.domain.search.repository.SearchHistoryRepository
import javax.inject.Inject

/** Очищает историю поисковых запросов каталога. */
class ClearSearchHistoryUseCase @Inject constructor(
    private val repository: SearchHistoryRepository,
) {
    suspend operator fun invoke() = repository.clear()
}
