package cz.miroslavpasek.pigeonnavigator.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CircularActionButton(
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    SmallFloatingActionButton(
        onClick = onClick,
        modifier = Modifier.size(BubbleSize),
        containerColor = colors.surfaceColorAtElevation(10.dp),
        contentColor = colors.onSurface,
        shape = CircleShape
    ) {
        icon()
    }
}
