package su.afk.yummy.tv.feature.search

import kotlinx.coroutines.flow.StateFlow

/**
 * Opens the dedicated search tab and requests focus in its query field.
 *
 * The pending request is state-backed so it survives navigation composition: the destination
 * may be composed after [openSearch] has been called.
 */
interface ISearchOpener {
    val isSearchFocusPending: StateFlow<Boolean>

    fun openSearch()

    /** Called by the search screen after it has moved focus to the query field. */
    fun consumeSearchFocus()
}
