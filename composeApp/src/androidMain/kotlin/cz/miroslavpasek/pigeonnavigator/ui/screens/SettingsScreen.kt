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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import androidx.compose.ui.unit.dp
import cz.miroslavpasek.pigeonnavigator.domain.settings.AltitudeUnit
import cz.miroslavpasek.pigeonnavigator.domain.settings.AppSettingsRepository
import cz.miroslavpasek.pigeonnavigator.domain.settings.DistanceUnit
import cz.miroslavpasek.pigeonnavigator.domain.settings.MAX_BEARING_UPDATE_THRESHOLD_DEGREES
import cz.miroslavpasek.pigeonnavigator.domain.settings.MAX_MAX_DYNAMIC_ZOOM_SPEED_KMH
import cz.miroslavpasek.pigeonnavigator.domain.settings.MAX_MAX_SPEED_ZOOM_OUT_DELTA
import cz.miroslavpasek.pigeonnavigator.domain.settings.MAX_MINIMUM_SEARCH_QUERY_LENGTH
import cz.miroslavpasek.pigeonnavigator.domain.settings.MAX_SEARCH_DEBOUNCE_MILLIS
import cz.miroslavpasek.pigeonnavigator.domain.settings.MAX_TIME_TO_COLLISION_WARNING_SECONDS
import cz.miroslavpasek.pigeonnavigator.domain.settings.MIN_BEARING_UPDATE_THRESHOLD_DEGREES
import cz.miroslavpasek.pigeonnavigator.domain.settings.MIN_MAX_DYNAMIC_ZOOM_SPEED_KMH
import cz.miroslavpasek.pigeonnavigator.domain.settings.MIN_MAX_SPEED_ZOOM_OUT_DELTA
import cz.miroslavpasek.pigeonnavigator.domain.settings.MIN_MINIMUM_SEARCH_QUERY_LENGTH
import cz.miroslavpasek.pigeonnavigator.domain.settings.MIN_SEARCH_DEBOUNCE_MILLIS
import cz.miroslavpasek.pigeonnavigator.domain.settings.MIN_TIME_TO_COLLISION_WARNING_SECONDS
import cz.miroslavpasek.pigeonnavigator.domain.settings.MapPreferences
import cz.miroslavpasek.pigeonnavigator.domain.settings.SearchPreferences
import cz.miroslavpasek.pigeonnavigator.domain.settings.SpeedUnit
import cz.miroslavpasek.pigeonnavigator.domain.settings.UnitPreferences
import cz.miroslavpasek.pigeonnavigator.domain.settings.WarningPreferences
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext
import java.util.Locale
import kotlin.math.roundToInt

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

                HorizontalDivider()

                SearchSection(
                    search = settings.search,
                    onSearchChange = { newSearch ->
                        scope.launch { repository.updateSearchPreferences(newSearch) }
                    }
                )

                HorizontalDivider()

                MapSection(
                    map = settings.map,
                    onMapChange = { newMap ->
                        scope.launch { repository.updateMapPreferences(newMap) }
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
        mutableStateOf(warning.timeToCollisionWarningSeconds.toFloat())
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("Terrain warning")
        Text(
            text = "Time-to-collision threshold: ${draft.roundToInt()} s",
            style = MaterialTheme.typography.bodyMedium,
        )
        Slider(
            value = draft,
            onValueChange = { draft = it },
            onValueChangeFinished = {
                onWarningChange(
                    WarningPreferences(timeToCollisionWarningSeconds = draft.roundToInt())
                )
            },
            valueRange = MIN_TIME_TO_COLLISION_WARNING_SECONDS.toFloat()..
                MAX_TIME_TO_COLLISION_WARNING_SECONDS.toFloat(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SearchSection(
    search: SearchPreferences,
    onSearchChange: (SearchPreferences) -> Unit,
) {
    var debounceDraft by remember(search.searchDebounceMillis) {
        mutableStateOf(search.searchDebounceMillis.toFloat())
    }
    var minLengthDraft by remember(search.minimumQueryLength) {
        mutableStateOf(search.minimumQueryLength.toFloat())
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("Search")
        Text(
            text = "Debounce: ${debounceDraft.roundToInt()} ms",
            style = MaterialTheme.typography.bodyMedium,
        )
        Slider(
            value = debounceDraft,
            onValueChange = { debounceDraft = it },
            onValueChangeFinished = {
                onSearchChange(
                    search.copy(searchDebounceMillis = debounceDraft.roundToInt().toLong())
                )
            },
            valueRange = MIN_SEARCH_DEBOUNCE_MILLIS.toFloat()..
                MAX_SEARCH_DEBOUNCE_MILLIS.toFloat(),
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = "Minimum query length: ${minLengthDraft.roundToInt()}",
            style = MaterialTheme.typography.bodyMedium,
        )
        Slider(
            value = minLengthDraft,
            onValueChange = { minLengthDraft = it },
            onValueChangeFinished = {
                onSearchChange(
                    search.copy(minimumQueryLength = minLengthDraft.roundToInt())
                )
            },
            valueRange = MIN_MINIMUM_SEARCH_QUERY_LENGTH.toFloat()..
                MAX_MINIMUM_SEARCH_QUERY_LENGTH.toFloat(),
            steps = MAX_MINIMUM_SEARCH_QUERY_LENGTH - MIN_MINIMUM_SEARCH_QUERY_LENGTH - 1,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MapSection(
    map: MapPreferences,
    onMapChange: (MapPreferences) -> Unit,
) {
    var maxSpeedDraft by remember(map.maxDynamicZoomSpeedKmh) {
        mutableStateOf(map.maxDynamicZoomSpeedKmh.toFloat())
    }
    var deltaDraft by remember(map.maxSpeedZoomOutDelta) {
        mutableStateOf(map.maxSpeedZoomOutDelta.toFloat())
    }
    var bearingDraft by remember(map.bearingUpdateThresholdDegrees) {
        mutableStateOf(map.bearingUpdateThresholdDegrees.toFloat())
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("Map")

        Text(
            text = "Max dynamic-zoom speed: ${maxSpeedDraft.roundToInt()} km/h",
            style = MaterialTheme.typography.bodyMedium,
        )
        Slider(
            value = maxSpeedDraft,
            onValueChange = { maxSpeedDraft = it },
            onValueChangeFinished = {
                onMapChange(map.copy(maxDynamicZoomSpeedKmh = maxSpeedDraft.toDouble()))
            },
            valueRange = MIN_MAX_DYNAMIC_ZOOM_SPEED_KMH.toFloat()..
                MAX_MAX_DYNAMIC_ZOOM_SPEED_KMH.toFloat(),
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = "Max speed zoom-out delta: ${String.format(Locale.US, "%.2f", deltaDraft)}",
            style = MaterialTheme.typography.bodyMedium,
        )
        Slider(
            value = deltaDraft,
            onValueChange = { deltaDraft = it },
            onValueChangeFinished = {
                onMapChange(map.copy(maxSpeedZoomOutDelta = deltaDraft.toDouble()))
            },
            valueRange = MIN_MAX_SPEED_ZOOM_OUT_DELTA.toFloat()..
                MAX_MAX_SPEED_ZOOM_OUT_DELTA.toFloat(),
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = "Bearing update threshold: ${String.format(Locale.US, "%.1f", bearingDraft)}\u00B0",
            style = MaterialTheme.typography.bodyMedium,
        )
        Slider(
            value = bearingDraft,
            onValueChange = { bearingDraft = it },
            onValueChangeFinished = {
                onMapChange(map.copy(bearingUpdateThresholdDegrees = bearingDraft.toDouble()))
            },
            valueRange = MIN_BEARING_UPDATE_THRESHOLD_DEGREES.toFloat()..
                MAX_BEARING_UPDATE_THRESHOLD_DEGREES.toFloat(),
            modifier = Modifier.fillMaxWidth(),
        )

        AssistChip(
            onClick = {
                onMapChange(MapPreferences())
            },
            label = { Text("Reset to defaults") },
            colors = AssistChipDefaults.assistChipColors()
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
