package su.afk.yummy.tv.domain.search.usecase

import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.domain.search.repository.SearchHistoryRepository
import javax.inject.Inject

/** Наблюдает за историей поисковых запросов каталога: последние запросы, от новых к старым. */
class ObserveSearchHistoryUseCase @Inject constructor(
    private val repository: SearchHistoryRepository,
) {
    operator fun invoke(): Flow<List<String>> = repository.observe()
}
