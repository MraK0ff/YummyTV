package su.afk.yummy.tv.core.designsystem.insets

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.graphics.Rect
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.Insets
import androidx.core.view.DisplayCutoutCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Current window geometry and system inset snapshot, in physical pixels.
 *
 * The Compose [WindowInsets.safeDrawing] source remains the preferred layout API because it
 * participates in Compose's inset consumption. This snapshot is for diagnostics and decisions
 * that need native cutout geometry or the individual system inset sources.
 */
data class AdaptiveWindowInfo(
    val windowWidthPx: Int = 0,
    val windowHeightPx: Int = 0,
    val displayWidthPx: Int = 0,
    val displayHeightPx: Int = 0,
    val orientation: Int = Configuration.ORIENTATION_UNDEFINED,
    val isFullscreen: Boolean = false,
    val systemBarsVisible: Boolean = true,
    val isInMultiWindowMode: Boolean = false,
    val hasDisplayCutout: Boolean = false,
    val cutoutBounds: List<Rect> = emptyList(),
    val safeInsets: Insets = Insets.NONE,
    val displayCutoutInsets: Insets = Insets.NONE,
    val statusBarInsets: Insets = Insets.NONE,
    val navigationBarInsets: Insets = Insets.NONE,
    val systemBarInsets: Insets = Insets.NONE,
    val imeInsets: Insets = Insets.NONE,
    val waterfallInsets: Insets = Insets.NONE,
) {
    val isLandscape: Boolean get() = windowWidthPx > windowHeightPx
    val isPortrait: Boolean get() = windowHeightPx >= windowWidthPx
}

val LocalAdaptiveWindowInfo = compositionLocalOf { AdaptiveWindowInfo() }

/**
 * Installs one lifecycle-scoped native inset observer on the activity decor view and exposes
 * the latest snapshot to descendants. Returning the original insets preserves normal dispatch
 * to AndroidX Activity and Compose; the observer never consumes or modifies them.
 *
 * Use once at the Activity's Compose root. Do not install competing inset listeners in screens.
 */
@Composable
fun AdaptiveWindowInsetsProvider(content: @Composable () -> Unit) {
    val composeView = LocalView.current
    val decorView = remember(composeView) { composeView.rootView }
    val activity = remember(decorView) { decorView.context.findActivity() }
    var info by remember(decorView) { mutableStateOf(AdaptiveWindowInfo()) }

    DisposableEffect(decorView, activity) {
        fun update(insets: WindowInsetsCompat?) {
            val cutout: DisplayCutoutCompat? = insets?.displayCutout
            val systemBars = insets?.getInsets(WindowInsetsCompat.Type.systemBars()) ?: Insets.NONE
            val statusBars = insets?.getInsets(WindowInsetsCompat.Type.statusBars()) ?: Insets.NONE
            val navigationBars = insets?.getInsets(WindowInsetsCompat.Type.navigationBars()) ?: Insets.NONE
            val ime = insets?.getInsets(WindowInsetsCompat.Type.ime()) ?: Insets.NONE
            val cutoutInsets = insets?.getInsets(WindowInsetsCompat.Type.displayCutout()) ?: Insets.NONE
            val waterfall = cutout?.waterfallInsets ?: Insets.NONE
            val safe = maxInsets(maxInsets(systemBars, cutoutInsets), waterfall)
            val bounds = cutout?.boundingRects?.map { Rect(it) }.orEmpty()
            val barsVisible = insets?.isVisible(WindowInsetsCompat.Type.systemBars()) ?: true
            val fullscreenFlag = ((activity?.window?.attributes?.flags ?: 0) and
                WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0

            val next = AdaptiveWindowInfo(
                windowWidthPx = decorView.width,
                windowHeightPx = decorView.height,
                displayWidthPx = decorView.resources.displayMetrics.widthPixels,
                displayHeightPx = decorView.resources.displayMetrics.heightPixels,
                orientation = decorView.resources.configuration.orientation,
                isFullscreen = fullscreenFlag || !barsVisible,
                systemBarsVisible = barsVisible,
                isInMultiWindowMode = activity?.isInMultiWindowMode == true,
                hasDisplayCutout = cutout != null || bounds.isNotEmpty() ||
                    cutoutInsets != Insets.NONE ||
                    waterfall != Insets.NONE,
                cutoutBounds = bounds,
                safeInsets = safe,
                displayCutoutInsets = cutoutInsets,
                statusBarInsets = statusBars,
                navigationBarInsets = navigationBars,
                systemBarInsets = systemBars,
                imeInsets = ime,
                waterfallInsets = waterfall,
            )
            if (next != info) info = next
        }

        val layoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            update(ViewCompat.getRootWindowInsets(decorView))
        }

        ViewCompat.setOnApplyWindowInsetsListener(decorView) { _, insets ->
            update(insets)
            insets
        }
        decorView.addOnLayoutChangeListener(layoutListener)
        update(ViewCompat.getRootWindowInsets(decorView))
        ViewCompat.requestApplyInsets(decorView)

        onDispose {
            ViewCompat.setOnApplyWindowInsetsListener(decorView, null)
            decorView.removeOnLayoutChangeListener(layoutListener)
        }
    }

    CompositionLocalProvider(LocalAdaptiveWindowInfo provides info, content = content)
}

private fun maxInsets(first: Insets, second: Insets): Insets = Insets.of(
    maxOf(first.left, second.left),
    maxOf(first.top, second.top),
    maxOf(first.right, second.right),
    maxOf(first.bottom, second.bottom),
)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Maps each cutout rectangle to the nearest physical window edge. Some devices report a
 * landscape cutout's safe area as top/bottom insets after rotation; using the rectangle keeps
 * padding on the edge where the camera actually is.
 */
private fun AdaptiveWindowInfo.edgeSpecificCutoutInsets(): Insets {
    if (windowWidthPx <= 0 || windowHeightPx <= 0 || cutoutBounds.isEmpty()) return Insets.NONE

    var left = 0
    var top = 0
    var right = 0
    var bottom = 0

    cutoutBounds.forEach { bounds ->
        val distances = listOf(
            bounds.left.coerceAtLeast(0),
            bounds.top.coerceAtLeast(0),
            (windowWidthPx - bounds.right).coerceAtLeast(0),
            (windowHeightPx - bounds.bottom).coerceAtLeast(0),
        )
        when (distances.indices.minByOrNull { distances[it] }) {
            0 -> left = maxOf(left, bounds.right, displayCutoutInsets.left)
            1 -> top = maxOf(top, bounds.bottom, displayCutoutInsets.top)
            2 -> right = maxOf(right, windowWidthPx - bounds.left, displayCutoutInsets.right)
            3 -> bottom = maxOf(bottom, windowHeightPx - bounds.top, displayCutoutInsets.bottom)
        }
    }
    return Insets.of(left, top, right, bottom)
}

@Composable
fun Modifier.adaptiveSafeDrawingPadding(
    sides: WindowInsetsSides = WindowInsetsSides.Horizontal + WindowInsetsSides.Vertical,
): Modifier {
    val cutoutInsets = LocalAdaptiveWindowInfo.current.edgeSpecificCutoutInsets()
    return windowInsetsPadding(
        WindowInsets(
            left = cutoutInsets.left,
            top = cutoutInsets.top,
            right = cutoutInsets.right,
            bottom = cutoutInsets.bottom,
        ).only(sides),
    )
}

/** Applies only the cutout's physical edge to interactive controls, never system-bar spacing. */
@Composable
fun Modifier.adaptiveSafeContentPadding(
    sides: WindowInsetsSides = WindowInsetsSides.Horizontal + WindowInsetsSides.Vertical,
): Modifier = adaptiveSafeDrawingPadding(sides)
