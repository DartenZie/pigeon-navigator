package cz.miroslavpasek.pigeonnavigator.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cz.miroslavpasek.pigeonnavigator.bridge.MapTapLookupState
import cz.miroslavpasek.pigeonnavigator.domain.aviation.Airspace
import cz.miroslavpasek.pigeonnavigator.domain.aviation.NearbyAirport
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockPoiItem
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockRoute
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockState
import kotlin.math.roundToInt

@Composable
fun SearchDock(
    state: SearchDockState,
    mapTapLookup: MapTapLookupState,
    maxPanelHeight: Dp,
    onExpandedChange: (Boolean) -> Unit,
    onQueryChanged: (String) -> Unit,
    onSubmitSearch: () -> Unit,
    onClearSearch: () -> Unit,
    onRoutePlanningChanged: (Boolean) -> Unit,
    onRouteSelected: (SearchDockRoute) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    val panelHeight by animateDpAsState(
        targetValue = if (state.isExpanded) {
            maxPanelHeight.coerceAtLeast(260.dp)
        } else {
            64.dp
        },
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 520f),
        label = "searchDockHeight"
    )

    val title = when (state.activeRoute) {
        SearchDockRoute.NearbyPoi -> "Nearby POIs"
        SearchDockRoute.SearchResults -> "Search Results"
        SearchDockRoute.RoutePlanner -> "Route Planner"
        SearchDockRoute.MapPointDetails -> "Tapped Point"
    }

    Surface(
        modifier = modifier
            .heightIn(min = 64.dp)
            .height(panelHeight)
            .widthIn(max = 360.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(if (state.isExpanded) 22.dp else 32.dp),
        color = colors.surfaceColorAtElevation(10.dp),
        tonalElevation = 10.dp,
        shadowElevation = 3.dp,
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandedChange(!state.isExpanded) }
            ) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .width(34.dp)
                        .padding(top = 6.dp)
                        .height(4.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                    color = colors.outline.copy(alpha = 0.55f)
                ) {}

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (state.activeRoute == SearchDockRoute.RoutePlanner) Icons.Filled.Route else Icons.Filled.Search,
                        contentDescription = null,
                        tint = colors.onSurface
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = if (state.isExpanded) "Collapse" else "Expand",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.primary
                    )
                }
            }

            if (state.isExpanded) {
                ExpandedDockContent(
                    state = state,
                    mapTapLookup = mapTapLookup,
                    onQueryChanged = onQueryChanged,
                    onSubmitSearch = onSubmitSearch,
                    onClearSearch = onClearSearch,
                    onRoutePlanningChanged = onRoutePlanningChanged,
                    onRouteSelected = onRouteSelected,
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun ExpandedDockContent(
    state: SearchDockState,
    mapTapLookup: MapTapLookupState,
    onQueryChanged: (String) -> Unit,
    onSubmitSearch: () -> Unit,
    onClearSearch: () -> Unit,
    onRoutePlanningChanged: (Boolean) -> Unit,
    onRouteSelected: (SearchDockRoute) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Column(modifier = modifier) {
        HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))

        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onQueryChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            singleLine = true,
            label = { Text("Search") }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TextButton(onClick = onSubmitSearch) {
                Text("Find")
            }
            TextButton(onClick = onClearSearch) {
                Text("Clear")
            }
            TextButton(onClick = { onRoutePlanningChanged(!state.isRoutePlanning) }) {
                Text(if (state.isRoutePlanning) "Stop plan" else "Plan route")
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.availableRoutes.forEach { route ->
                        FilterChip(
                            selected = route == state.activeRoute,
                            onClick = { onRouteSelected(route) },
                            label = {
                                Text(route.label())
                            }
                        )
                    }
                }
            }

            when (state.activeRoute) {
                SearchDockRoute.NearbyPoi -> {
                    item {
                        SectionTitle(text = "Nearby Points of Interest")
                    }
                    if (state.isNearbyPoiLoading) {
                        item {
                            EmptyLine("Fetching nearby POIs...")
                        }
                    }
                    state.nearbyPoiErrorMessage?.let { error ->
                        item {
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.error,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                    if (!state.isNearbyPoiLoading && state.nearbyPoiItems.isEmpty()) {
                        item {
                            EmptyLine("No nearby POIs available")
                        }
                    } else {
                        items(state.nearbyPoiItems, key = { it.id }) { item ->
                            NearbyPoiRow(item = item)
                        }
                    }
                }

                SearchDockRoute.SearchResults -> {
                    item {
                        SectionTitle(text = "Search")
                    }
                    if (state.isSearching) {
                        item {
                            EmptyLine("Searching...")
                        }
                    }
                    state.searchErrorMessage?.let { error ->
                        item {
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.error,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                    if (!state.isSearching && state.searchResults.isEmpty()) {
                        item {
                            EmptyLine("No search results")
                        }
                    } else {
                        items(state.searchResults, key = { it }) { result ->
                            Text(
                                text = result,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                SearchDockRoute.RoutePlanner -> {
                    item {
                        SectionTitle(text = "Route Planner")
                    }
                    item {
                        EmptyLine("Route planner workspace active")
                    }
                    item {
                        EmptyLine("Add route planning controls here")
                    }
                }

                SearchDockRoute.MapPointDetails -> {
                    val selectedLatitude = mapTapLookup.selectedLatitude
                    val selectedLongitude = mapTapLookup.selectedLongitude
                    item {
                        SectionTitle(text = "Tapped Coordinates")
                    }
                    if (selectedLatitude != null && selectedLongitude != null) {
                        item {
                            EmptyLine(
                                formatCoordinateLabel(
                                    latitude = selectedLatitude,
                                    longitude = selectedLongitude
                                )
                            )
                        }
                    } else {
                        item {
                            EmptyLine("Tap on the map to inspect this route")
                        }
                    }

                    if (mapTapLookup.isLoading) {
                        item {
                            EmptyLine("Fetching nearby data...")
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
                            EmptyLine("No airports near this point")
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
                            EmptyLine("No airspaces contain this point")
                        }
                    } else {
                        items(mapTapLookup.airspaces, key = { it.id }) { airspace ->
                            AirspaceRow(airspace = airspace)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(18.dp))
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
private fun NearbyPoiRow(item: SearchDockPoiItem) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
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
            Text(text = item.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = "${item.kindLabel} · ${item.subtitle}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                maxLines = 1
            )
        }
        Text(
            text = formatDistance(item.distanceMeters),
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant
        )
    }
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

private fun formatAltitudeBand(airspace: Airspace): String {
    val lower = airspace.lowerLimitMeters?.let { "${it}m ${airspace.lowerLimitReference ?: ""}".trim() } ?: "SFC"
    val upper = airspace.upperLimitMeters?.let { "${it}m ${airspace.upperLimitReference ?: ""}".trim() } ?: "UNL"
    return "$lower - $upper"
}

private fun formatCoordinateLabel(latitude: Double, longitude: Double): String {
    return "%.4f, %.4f".format(latitude, longitude)
}

private fun formatDistance(distanceMeters: Double): String {
    return if (distanceMeters >= 1000.0) {
        "${(distanceMeters / 1000.0 * 10.0).roundToInt() / 10.0} km"
    } else {
        "${distanceMeters.roundToInt()} m"
    }
}

private fun SearchDockRoute.label(): String = when (this) {
    SearchDockRoute.NearbyPoi -> "Nearby"
    SearchDockRoute.SearchResults -> "Search"
    SearchDockRoute.RoutePlanner -> "Plan"
    SearchDockRoute.MapPointDetails -> "Map Tap"
}
