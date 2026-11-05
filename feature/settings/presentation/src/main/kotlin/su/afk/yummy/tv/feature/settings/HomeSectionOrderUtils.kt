package su.afk.yummy.tv.feature.settings

import su.afk.yummy.tv.feature.settings.model.DetailsButtonMoveDirection

/** Moves a Home section without changing the relative order of any other sections. */
internal fun List<HomeSectionKind>.movedHomeSection(
    section: HomeSectionKind,
    direction: DetailsButtonMoveDirection,
): List<HomeSectionKind> {
    val index = indexOf(section)
    if (index < 0) return this
    val target = when (direction) {
        DetailsButtonMoveDirection.UP -> index - 1
        DetailsButtonMoveDirection.DOWN -> index + 1
    }
    if (target !in indices) return this
    return toMutableList().apply {
        this[index] = this[target]
        this[target] = section
    }
}
