package cz.miroslavpasek.pigeonnavigator.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cz.miroslavpasek.pigeonnavigator.bridge.MapTapLookupState
import cz.miroslavpasek.pigeonnavigator.domain.aviation.Airspace
import cz.miroslavpasek.pigeonnavigator.domain.aviation.NearbyAirport
import kotlin.math.roundToInt

private enum class PanelStage {
    Collapsed,
    Partial,
    Full
}

@Composable
fun SearchDock(
    mapTapLookup: MapTapLookupState,
    maxPanelHeight: Dp,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    var stage by remember { mutableStateOf(PanelStage.Collapsed) }
    val listState = rememberLazyListState()

    val hasSelection = mapTapLookup.selectedLatitude != null && mapTapLookup.selectedLongitude != null

    LaunchedEffect(hasSelection) {
        stage = if (hasSelection) PanelStage.Partial else PanelStage.Collapsed
    }

    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset, stage) {
        if (
            stage == PanelStage.Partial &&
            (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 4)
        ) {
            stage = PanelStage.Full
        }
    }

    val panelHeight by animateDpAsState(
        targetValue = when (stage) {
            PanelStage.Collapsed -> 64.dp
            PanelStage.Partial -> (maxPanelHeight * 0.5f).coerceAtLeast(220.dp)
            PanelStage.Full -> maxPanelHeight
        },
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 520f),
        label = "searchDockHeight"
    )

    val cornerRadius = if (stage == PanelStage.Full) 22.dp else 32.dp

    Surface(
        modifier = modifier
            .heightIn(min = 64.dp)
            .height(panelHeight)
            .widthIn(max = 332.dp),
        shape = RoundedCornerShape(cornerRadius),
        color = colors.surfaceColorAtElevation(10.dp),
        tonalElevation = 10.dp,
        shadowElevation = 3.dp,
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .width(34.dp)
                        .padding(top = 6.dp)
                        .height(4.dp),
                    shape = RoundedCornerShape(50),
                    color = colors.outline.copy(alpha = 0.55f)
                ) {}

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = colors.onSurface
                    )
                    val title = if (!hasSelection) {
                        "Search"
                    } else {
                        formatCoordinateLabel(
                            latitude = mapTapLookup.selectedLatitude ?: 0.0,
                            longitude = mapTapLookup.selectedLongitude ?: 0.0
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                }
            }

            if (stage != PanelStage.Collapsed) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 10.dp)
                        .fillMaxHeight()
                ) {
                    HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (mapTapLookup.isLoading) {
                            item {
                                Text(
                                    text = "Fetching nearby data...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }

                        mapTapLookup.errorMessage?.let { error ->
                            item {
                                Text(
                                    text = error,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.error,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                        }

                        item {
                            SectionTitle(text = "Airports")
                        }

                        if (mapTapLookup.airports.isEmpty()) {
                            item {
                                EmptyLine(text = "No airports near this point")
                            }
                        } else {
                            items(mapTapLookup.airports, key = { it.airport.id }) { airport ->
                                AirportRow(airport = airport)
                            }
                        }

                        item {
                            SectionTitle(text = "Airspaces")
                        }

                        if (mapTapLookup.airspaces.isEmpty()) {
                            item {
                                EmptyLine(text = "No airspaces contain this point")
                            }
                        } else {
                            items(mapTapLookup.airspaces, key = { it.id }) { airspace ->
                                AirspaceRow(airspace = airspace)
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 8.dp)
    )
}

@Composable
private fun EmptyLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 8.dp)
    )
}

@Composable
private fun AirportRow(airport: NearbyAirport) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = colors.primary
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = airport.airport.id, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = airport.airport.name,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                maxLines = 1
            )
        }
        Text(
            text = formatDistance(airport.distanceMeters),
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant
        )
    }
}

@Composable
private fun AirspaceRow(airspace: Airspace) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = airspace.name.ifBlank { airspace.id }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Text(
            text = "${airspace.kind} · ${formatAltitudeBand(airspace)}",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant
        )
    }
}

private fun formatDistance(distanceMeters: Double): String {
    return if (distanceMeters >= 1000.0) {
        "${(distanceMeters / 1000.0 * 10.0).roundToInt() / 10.0} km"
    } else {
        "${distanceMeters.roundToInt()} m"
    }
}

private fun formatAltitudeBand(airspace: Airspace): String {
    val lower = airspace.lowerLimitMeters?.let { "${it}m ${airspace.lowerLimitReference ?: ""}".trim() } ?: "SFC"
    val upper = airspace.upperLimitMeters?.let { "${it}m ${airspace.upperLimitReference ?: ""}".trim() } ?: "UNL"
    return "$lower - $upper"
}

private fun formatCoordinateLabel(latitude: Double, longitude: Double): String {
    return "%.4f, %.4f".format(latitude, longitude)
}
