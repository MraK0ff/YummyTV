package su.afk.yummy.tv.feature.search.mapper

import su.afk.yummy.tv.core.utils.paging.OffsetPage
import su.afk.yummy.tv.domain.search.model.SearchItem
import su.afk.yummy.tv.domain.search.model.SearchPage

internal fun SearchPage.toOffsetPage(): OffsetPage<SearchItem> =
    OffsetPage(
        items = items,
        nextOffset = nextOffset,
        canLoadMore = canLoadMore,
    )
