package su.afk.yummy.tv.feature.search

import androidx.navigation3.runtime.NavKey

/** Creates a search destination, optionally pre-filled with a query. */
interface ISearchNavigator {
    fun getSearchDest(initialQuery: String = ""): NavKey
}
