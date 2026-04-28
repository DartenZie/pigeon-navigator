package cz.miroslavpasek.pigeonnavigator.ui.common.theme

import androidx.compose.ui.unit.dp

/**
 * Shared spacing tokens for the Android UI.
 *
 * Use these instead of hard-coded `Modifier.padding(16.dp)` etc. Only swap a literal
 * for a token when the value actually matches; do not invent tokens to chase
 * one-off measurements.
 */
object AppSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
}
