package su.afk.yummy.tv.domain.search.usecase

import su.afk.yummy.tv.domain.search.model.CachedSearchPage
import su.afk.yummy.tv.domain.search.model.SearchFilters
import su.afk.yummy.tv.domain.search.repository.SearchRepository
import javax.inject.Inject

/**
 * Читает страницу выдачи только из локального кэша, без обращения к сети, и сообщает,
 * свежа ли она. Нужна, чтобы показать прошлую выдачу сразу, пока [SearchUseCase] обновляет её.
 * Возвращает `null`, если такой страницы в кэше нет.
 */
class GetCachedSearchPageUseCase @Inject constructor(private val repository: SearchRepository) {
    suspend operator fun invoke(
        query: String,
        filters: SearchFilters = SearchFilters.EMPTY,
        limit: Int = 40,
        offset: Int = 0,
    ): CachedSearchPage? = repository.getCachedSearchPage(query, filters, limit, offset)
}
