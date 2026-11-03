package su.afk.yummy.tv.data.search.mapper

import su.afk.yummy.tv.core.utils.poster.animePosterOrNull
import su.afk.yummy.tv.data.search.dto.YaniSearchItemDto
import su.afk.yummy.tv.domain.catalog.model.SearchItem

internal fun YaniSearchItemDto.toSearchItem(): SearchItem? {
    val id = animeId ?: return null
    return SearchItem(
        id = id,
        title = title,
        description = description,
        poster = poster?.let { animePosterOrNull(it.small, it.medium, it.big, it.fullsize, it.mega) },
        rating = rating?.average?.takeIf { it > 0.0 },
        year = year,
    )
}
