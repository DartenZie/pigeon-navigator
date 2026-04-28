package cz.miroslavpasek.pigeonnavigator.ui.settings.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Title text for a settings section (e.g. "Units", "Terrain warning").
 *
 * Kept inside `settings/component/` because it is only used by the settings
 * screen today. Promote to `common/component/` once another feature reuses it.
 */
@Composable
internal fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier,
    )
}
