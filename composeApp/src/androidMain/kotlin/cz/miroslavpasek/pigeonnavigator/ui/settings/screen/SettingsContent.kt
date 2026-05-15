package cz.miroslavpasek.pigeonnavigator.ui.settings.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cz.miroslavpasek.pigeonnavigator.domain.settings.AppSettings
import cz.miroslavpasek.pigeonnavigator.domain.settings.LocationPreferences
import cz.miroslavpasek.pigeonnavigator.domain.settings.UnitPreferences
import cz.miroslavpasek.pigeonnavigator.domain.settings.WarningPreferences
import cz.miroslavpasek.pigeonnavigator.ui.common.theme.AppSpacing
import cz.miroslavpasek.pigeonnavigator.ui.settings.section.LocationSection
import cz.miroslavpasek.pigeonnavigator.ui.settings.section.UnitsSection
import cz.miroslavpasek.pigeonnavigator.ui.settings.section.WarningSection

/**
 * High-level layout for the settings screen.
 *
 * Composes the top app bar, the scrollable column, and the individual sections.
 * Knows nothing about how settings are persisted — that wiring lives in
 * [SettingsScreen].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    settings: AppSettings,
    onClose: () -> Unit,
    onUnitsChange: (UnitPreferences) -> Unit,
    onWarningChange: (WarningPreferences) -> Unit,
    onLocationChange: (LocationPreferences) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Close settings")
                    }
                },
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AppSpacing.md, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                UnitsSection(
                    units = settings.units,
                    onUnitsChange = onUnitsChange,
                )

                HorizontalDivider()

                WarningSection(
                    warning = settings.warning,
                    onWarningChange = onWarningChange,
                )

                HorizontalDivider()

                LocationSection(
                    location = settings.location,
                    onLocationChange = onLocationChange,
                )
            }
        }
    }
}
