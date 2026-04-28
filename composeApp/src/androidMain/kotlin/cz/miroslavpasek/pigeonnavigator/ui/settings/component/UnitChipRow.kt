package cz.miroslavpasek.pigeonnavigator.ui.settings.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cz.miroslavpasek.pigeonnavigator.ui.common.theme.AppSpacing

/**
 * Labelled row of [FilterChip]s used by the units section.
 *
 * Renders one chip per option; the currently [selected] entry is highlighted.
 */
@Composable
internal fun <T> UnitChipRow(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(text = label, style = MaterialTheme.typography.labelLarge)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelect(option) },
                label = { Text(optionLabel(option)) },
            )
        }
    }
}
