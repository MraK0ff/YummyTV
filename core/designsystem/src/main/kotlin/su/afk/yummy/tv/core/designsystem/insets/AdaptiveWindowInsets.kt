package su.afk.yummy.tv.core.designsystem.insets

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.ui.Modifier

/**
 * Applies only the requested sides of the current safe drawing insets.
 *
 * [WindowInsets.safeDrawing] is backed by Compose's platform inset source and updates as
 * system bars, display cutouts, waterfall edges, and window configuration change. Keep
 * decorative/media surfaces edge-to-edge and apply this to interactive content layers.
 */
fun Modifier.adaptiveSafeDrawingPadding(
    sides: WindowInsetsSides = WindowInsetsSides.All,
): Modifier = windowInsetsPadding(WindowInsets.safeDrawing.only(sides))
