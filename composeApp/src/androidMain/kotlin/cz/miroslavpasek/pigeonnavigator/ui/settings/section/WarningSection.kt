package cz.miroslavpasek.pigeonnavigator.ui.settings.section

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import cz.miroslavpasek.pigeonnavigator.domain.settings.MAX_TIME_TO_COLLISION_WARNING_SECONDS
import cz.miroslavpasek.pigeonnavigator.domain.settings.MIN_TIME_TO_COLLISION_WARNING_SECONDS
import cz.miroslavpasek.pigeonnavigator.domain.settings.WarningPreferences
import cz.miroslavpasek.pigeonnavigator.ui.common.theme.AppSpacing
import cz.miroslavpasek.pigeonnavigator.ui.settings.component.SectionHeader

/**
 * Edits the time-to-collision threshold used by terrain warnings.
 *
 * Validates the input locally and only forwards [onWarningChange] when the
 * parsed value lies inside the allowed domain range.
 */
@Composable
fun WarningSection(
    warning: WarningPreferences,
    onWarningChange: (WarningPreferences) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember(warning.timeToCollisionWarningSeconds) {
        mutableStateOf(warning.timeToCollisionWarningSeconds.toString())
    }
    val parsed = draft.toIntOrNull()
    val isError = parsed == null || parsed !in
        MIN_TIME_TO_COLLISION_WARNING_SECONDS..MAX_TIME_TO_COLLISION_WARNING_SECONDS

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
    ) {
        SectionHeader(title = "Terrain warning")
        OutlinedTextField(
            value = draft,
            onValueChange = { value ->
                val digitsOnly = value.filter(Char::isDigit)
                draft = digitsOnly
                digitsOnly.toIntOrNull()?.let { seconds ->
                    if (seconds in MIN_TIME_TO_COLLISION_WARNING_SECONDS..MAX_TIME_TO_COLLISION_WARNING_SECONDS) {
                        onWarningChange(WarningPreferences(timeToCollisionWarningSeconds = seconds))
                    }
                }
            },
            label = { Text("Time-to-collision") },
            suffix = { Text("s") },
            supportingText = {
                Text(
                    "${MIN_TIME_TO_COLLISION_WARNING_SECONDS}-" +
                        "${MAX_TIME_TO_COLLISION_WARNING_SECONDS} seconds"
                )
            },
            isError = isError,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
