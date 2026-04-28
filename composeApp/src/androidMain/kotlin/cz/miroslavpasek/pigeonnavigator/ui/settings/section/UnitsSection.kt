package cz.miroslavpasek.pigeonnavigator.ui.settings.section

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cz.miroslavpasek.pigeonnavigator.domain.settings.AltitudeUnit
import cz.miroslavpasek.pigeonnavigator.domain.settings.DistanceUnit
import cz.miroslavpasek.pigeonnavigator.domain.settings.SpeedUnit
import cz.miroslavpasek.pigeonnavigator.domain.settings.UnitPreferences
import cz.miroslavpasek.pigeonnavigator.ui.settings.component.SectionHeader
import cz.miroslavpasek.pigeonnavigator.ui.settings.component.UnitChipRow
import cz.miroslavpasek.pigeonnavigator.ui.settings.model.displayLabel

/**
 * Lets the user pick distance, altitude and speed units.
 *
 * Behaviour-preserving extraction from the original `SettingsScreen`.
 */
@Composable
fun UnitsSection(
    units: UnitPreferences,
    onUnitsChange: (UnitPreferences) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionHeader(title = "Units")

        UnitChipRow(
            label = "Distance",
            options = DistanceUnit.entries,
            selected = units.distance,
            optionLabel = { it.displayLabel() },
            onSelect = { onUnitsChange(units.copy(distance = it)) },
        )

        UnitChipRow(
            label = "Altitude",
            options = AltitudeUnit.entries,
            selected = units.altitude,
            optionLabel = { it.displayLabel() },
            onSelect = { onUnitsChange(units.copy(altitude = it)) },
        )

        UnitChipRow(
            label = "Speed",
            options = SpeedUnit.entries,
            selected = units.speed,
            optionLabel = { it.displayLabel() },
            onSelect = { onUnitsChange(units.copy(speed = it)) },
        )
    }
}
