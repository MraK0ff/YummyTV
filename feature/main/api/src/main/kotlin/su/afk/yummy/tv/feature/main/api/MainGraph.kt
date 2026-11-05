package su.afk.yummy.tv.feature.main.api

import androidx.compose.runtime.Composable

/** Root graph reports readiness only after the persisted appearance snapshot has been applied. */
interface MainGraph {
    @Composable
    fun MainGraph(onAppearanceReady: () -> Unit = {})
}
