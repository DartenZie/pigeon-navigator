package cz.miroslavpasek.pigeonnavigator.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cz.miroslavpasek.pigeonnavigator.domain.settings.AltitudeUnit
import cz.miroslavpasek.pigeonnavigator.domain.settings.AppSettingsRepository
import cz.miroslavpasek.pigeonnavigator.domain.settings.DistanceUnit
import cz.miroslavpasek.pigeonnavigator.domain.settings.MAX_TIME_TO_COLLISION_WARNING_SECONDS
import cz.miroslavpasek.pigeonnavigator.domain.settings.MIN_TIME_TO_COLLISION_WARNING_SECONDS
import cz.miroslavpasek.pigeonnavigator.domain.settings.SpeedUnit
import cz.miroslavpasek.pigeonnavigator.domain.settings.UnitPreferences
import cz.miroslavpasek.pigeonnavigator.domain.settings.WarningPreferences
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

/**
 * Full-screen settings editor backed by [AppSettingsRepository].
 *
 * Reads the current [cz.miroslavpasek.pigeonnavigator.domain.settings.AppSettings] via the
 * repository's `StateFlow` and forwards user edits back through the repository's `update*`
 * methods. The screen owns no business logic of its own – it is a thin presentation layer
 * over the existing domain contract.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val repository = remember { GlobalContext.get().get<AppSettingsRepository>() }
    val settings by repository.settings.collectAsState()
    val scope = rememberCoroutineScope()

    Surface(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Close settings")
                    }
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                UnitsSection(
                    units = settings.units,
                    onUnitsChange = { newUnits ->
                        scope.launch { repository.updateUnits(newUnits) }
                    }
                )

                HorizontalDivider()

                WarningSection(
                    warning = settings.warning,
                    onWarningChange = { newWarning ->
                        scope.launch { repository.updateWarningPreferences(newWarning) }
                    }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
    )
}

@Composable
private fun UnitsSection(
    units: UnitPreferences,
    onUnitsChange: (UnitPreferences) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("Units")

        Text("Distance", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DistanceUnit.entries.forEach { option ->
                FilterChip(
                    selected = units.distance == option,
                    onClick = { onUnitsChange(units.copy(distance = option)) },
                    label = { Text(option.displayLabel()) }
                )
            }
        }

        Text("Altitude", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AltitudeUnit.entries.forEach { option ->
                FilterChip(
                    selected = units.altitude == option,
                    onClick = { onUnitsChange(units.copy(altitude = option)) },
                    label = { Text(option.displayLabel()) }
                )
            }
        }

        Text("Speed", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SpeedUnit.entries.forEach { option ->
                FilterChip(
                    selected = units.speed == option,
                    onClick = { onUnitsChange(units.copy(speed = option)) },
                    label = { Text(option.displayLabel()) }
                )
            }
        }
    }
}

@Composable
private fun WarningSection(
    warning: WarningPreferences,
    onWarningChange: (WarningPreferences) -> Unit,
) {
    var draft by remember(warning.timeToCollisionWarningSeconds) {
        mutableStateOf(warning.timeToCollisionWarningSeconds.toString())
    }
    val parsed = draft.toIntOrNull()
    val isError = parsed == null || parsed !in
        MIN_TIME_TO_COLLISION_WARNING_SECONDS..MAX_TIME_TO_COLLISION_WARNING_SECONDS

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("Terrain warning")
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

private fun DistanceUnit.displayLabel(): String = when (this) {
    DistanceUnit.NauticalMiles -> "NM"
    DistanceUnit.Kilometers -> "km"
    DistanceUnit.Miles -> "mi"
}

private fun AltitudeUnit.displayLabel(): String = when (this) {
    AltitudeUnit.Feet -> "ft"
    AltitudeUnit.Meters -> "m"
}

private fun SpeedUnit.displayLabel(): String = when (this) {
    SpeedUnit.Knots -> "kt"
    SpeedUnit.KilometersPerHour -> "km/h"
    SpeedUnit.MilesPerHour -> "mph"
    SpeedUnit.MetersPerSecond -> "m/s"
}
