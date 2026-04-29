package cz.miroslavpasek.pigeonnavigator.ui.common.component

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cz.miroslavpasek.pigeonnavigator.ui.common.theme.AppDimensions

/**
 * Compact circular action button used by HUD overlays.
 *
 * Caller supplies the icon content; sizing and tonal styling are fixed so the
 * button visually matches [IndicatorBubble] and other bubble-shaped controls.
 */
@Composable
fun CircularActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    SmallFloatingActionButton(
        onClick = onClick,
        modifier = modifier.size(AppDimensions.BubbleSize),
        containerColor = colors.surfaceColorAtElevation(10.dp),
        contentColor = colors.onSurface,
        shape = CircleShape,
    ) {
        icon()
    }
}
