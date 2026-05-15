package cz.miroslavpasek.pigeonnavigator.ui.settings.section

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cz.miroslavpasek.pigeonnavigator.domain.settings.LocationPreferences
import cz.miroslavpasek.pigeonnavigator.domain.settings.LocationSource
import cz.miroslavpasek.pigeonnavigator.domain.settings.UdpLocationFormat
import cz.miroslavpasek.pigeonnavigator.ui.settings.component.SectionHeader
import cz.miroslavpasek.pigeonnavigator.ui.settings.component.UnitChipRow

@Composable
fun LocationSection(
    location: LocationPreferences,
    onLocationChange: (LocationPreferences) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionHeader(title = "Location")

        UnitChipRow(
            label = "Source",
            options = LocationSource.entries,
            selected = location.source,
            optionLabel = { it.displayLabel() },
            onSelect = { onLocationChange(location.copy(source = it)) },
        )

        if (location.source == LocationSource.Udp) {
            UnitChipRow(
                label = "UDP format",
                options = UdpLocationFormat.entries,
                selected = location.udpFormat,
                optionLabel = { it.displayLabel() },
                onSelect = { onLocationChange(location.copy(udpFormat = it)) },
            )
        }
    }
}

private fun LocationSource.displayLabel(): String = when (this) {
    LocationSource.DeviceGps -> "GPS"
    LocationSource.Udp -> "UDP"
}

private fun UdpLocationFormat.displayLabel(): String = when (this) {
    UdpLocationFormat.Msfs -> "MSFS"
    UdpLocationFormat.XPlane -> "X-Plane"
}
