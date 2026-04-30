package cz.miroslavpasek.pigeonnavigator.ui.searchdock.section

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cz.miroslavpasek.pigeonnavigator.bridge.MapTapLookupState
import cz.miroslavpasek.pigeonnavigator.domain.aviation.Airspace
import cz.miroslavpasek.pigeonnavigator.domain.aviation.NearbyAirport
import cz.miroslavpasek.pigeonnavigator.domain.aviation.NearbyNavaid
import cz.miroslavpasek.pigeonnavigator.domain.search.SearchResult
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockPoiItem
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockRoute
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockRoutePoint
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockState
import cz.miroslavpasek.pigeonnavigator.ui.searchdock.component.AirportRow
import cz.miroslavpasek.pigeonnavigator.ui.searchdock.component.AirspaceRow
import cz.miroslavpasek.pigeonnavigator.ui.searchdock.component.DetailHeader
import cz.miroslavpasek.pigeonnavigator.ui.searchdock.component.DetailRow
import cz.miroslavpasek.pigeonnavigator.ui.searchdock.component.EmptyLine
import cz.miroslavpasek.pigeonnavigator.ui.searchdock.component.MapTapDetailPanel
import cz.miroslavpasek.pigeonnavigator.ui.searchdock.component.NavaidRow
import cz.miroslavpasek.pigeonnavigator.ui.searchdock.component.NearbyPoiDetailPanel
import cz.miroslavpasek.pigeonnavigator.ui.searchdock.component.NearbyPoiRow
import cz.miroslavpasek.pigeonnavigator.ui.searchdock.component.RouteDestinationRow
import cz.miroslavpasek.pigeonnavigator.ui.searchdock.component.SearchResultDetailPanel
import cz.miroslavpasek.pigeonnavigator.ui.searchdock.component.SearchResultRow
import cz.miroslavpasek.pigeonnavigator.ui.searchdock.component.SectionTitle
import cz.miroslavpasek.pigeonnavigator.ui.searchdock.internal.MapTapRecord
import cz.miroslavpasek.pigeonnavigator.ui.searchdock.internal.findMapTapRecord
import cz.miroslavpasek.pigeonnavigator.ui.searchdock.internal.formatCoordinateLabel
import cz.miroslavpasek.pigeonnavigator.ui.searchdock.internal.routeLabelForIndex
import cz.miroslavpasek.pigeonnavigator.ui.searchdock.internal.toRoutePoint

/**
 * Renders the expanded body of the search dock (under the search bar).
 *
 * Picks one of four route-specific layouts based on
 * [SearchDockState.activeRoute] and shows a detail panel when the user has
 * tapped through to a specific record. Pure UI — no IO or business state
 * mutation.
 */
@Composable
internal fun SearchDockExpandedContent(
    state: SearchDockState,
    mapTapLookup: MapTapLookupState,
    onNearbyPoiSelected: (SearchDockPoiItem) -> Unit,
    onSearchResultSelected: (SearchResult) -> Unit,
    onMapTapAirportSelected: (NearbyAirport) -> Unit,
    onMapTapAirspaceSelected: (Airspace) -> Unit,
    onMapTapNavaidSelected: (NearbyNavaid) -> Unit,
    onMapTapDetailRequested: (key: String) -> Unit,
    onMapTapDetailClosed: () -> Unit,
    onNavigationDetailRequested: () -> Unit,
    onNavigationWaypointDetailRequested: (id: String) -> Unit,
    onAddWaypointRequested: () -> Unit,
    onEndFlightRequested: () -> Unit,
    onAddToRouteClicked: (SearchDockRoutePoint) -> Unit,
    onRouteDestinationRemoved: (String) -> Unit,
    onContentScrollStarted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    var selectedNearbyPoiId by remember { mutableStateOf<String?>(null) }
    var selectedSearchResultId by remember { mutableStateOf<String?>(null) }
    val selectedNearbyPoi = selectedNearbyPoiId?.let { id ->
        state.nearbyPoiItems.firstOrNull { it.id == id }
    }
    val selectedSearchResult = selectedSearchResultId?.let { id ->
        state.searchResults.firstOrNull { it.id == id }
    }
    val contentScrollConnection = remember(onContentScrollStarted) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available != Offset.Zero) {
                    onContentScrollStarted()
                }
                return Offset.Zero
            }
        }
    }

    if (state.activeRoute == SearchDockRoute.NavigationDetail) {
        NavigationDetailContent(
            state = state,
            onWaypointDetailRequested = onNavigationWaypointDetailRequested,
            onNavigationDetailClosed = onNavigationDetailRequested,
            onAddWaypointRequested = onAddWaypointRequested,
            onEndFlightRequested = onEndFlightRequested,
            modifier = modifier,
        )
        return
    }

    Column(modifier = modifier) {
        HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .nestedScroll(contentScrollConnection),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            when (state.activeRoute) {
                SearchDockRoute.Nearby -> {
                    if (selectedNearbyPoi != null) {
                        item {
                            NearbyPoiDetailPanel(
                                item = selectedNearbyPoi,
                                onBack = { selectedNearbyPoiId = null },
                                onLocateClicked = { onNearbyPoiSelected(selectedNearbyPoi) },
                                onAddToRouteClicked = {
                                    onAddToRouteClicked(selectedNearbyPoi.toRoutePoint())
                                },
                                showAddToRouteAction = !state.isRouteDestination(selectedNearbyPoi.id),
                            )
                        }
                    } else {
                        item { SectionTitle(text = "Nearby Points of Interest") }
                        if (state.isNearbyPoiLoading) {
                            item { EmptyLine("Fetching nearby POIs...") }
                        }
                        state.nearbyPoiErrorMessage?.let { error ->
                            item {
                                Text(
                                    text = error,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.error,
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                )
                            }
                        }
                        if (!state.isNearbyPoiLoading && state.nearbyPoiItems.isEmpty()) {
                            item { EmptyLine("No nearby POIs available") }
                        } else {
                            items(state.nearbyPoiItems, key = { it.id }) { item ->
                                NearbyPoiRow(
                                    item = item,
                                    onClick = { selectedNearbyPoiId = item.id },
                                )
                            }
                        }
                    }
                }

                SearchDockRoute.Search -> {
                    if (selectedSearchResult != null) {
                        item {
                            SearchResultDetailPanel(
                                result = selectedSearchResult,
                                onBack = { selectedSearchResultId = null },
                                onLocateClicked = { onSearchResultSelected(selectedSearchResult) },
                                onAddToRouteClicked = {
                                    onAddToRouteClicked(selectedSearchResult.toRoutePoint())
                                },
                                showAddToRouteAction = !state.isRouteDestination(selectedSearchResult.id),
                            )
                        }
                    } else {
                        item { SectionTitle(text = "Search") }
                        if (state.isSearching) {
                            item { EmptyLine("Searching...") }
                        }
                        state.searchErrorMessage?.let { error ->
                            item {
                                Text(
                                    text = error,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.error,
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                )
                            }
                        }
                        if (!state.isSearching && state.searchResults.isEmpty()) {
                            item { EmptyLine("No search results") }
                        } else {
                            items(state.searchResults, key = { it.id }) { result ->
                                SearchResultRow(
                                    result = result,
                                    onClick = { selectedSearchResultId = result.id },
                                )
                            }
                        }
                    }
                }

                SearchDockRoute.RoutePlanner -> {
                    item { SectionTitle(text = "Route Planner") }
                    if (state.routeDestinations.isEmpty()) {
                        item { EmptyLine("Add a point to start a route from your current location") }
                    } else {
                        item { EmptyLine("A Current Location") }
                        itemsIndexed(state.routeDestinations) { index, point ->
                            RouteDestinationRow(
                                label = routeLabelForIndex(index + 1),
                                point = point,
                                onRemove = { onRouteDestinationRemoved(point.id) },
                            )
                        }
                    }
                }

                SearchDockRoute.MapTap -> {
                    val detailRecord = state.selectedMapTapDetailKey?.let { key ->
                        findMapTapRecord(key = key, lookup = mapTapLookup)
                    }

                    if (detailRecord != null) {
                        item {
                            MapTapDetailPanel(
                                record = detailRecord,
                                onBack = onMapTapDetailClosed,
                                onLocateClicked = {
                                    when (detailRecord) {
                                        is MapTapRecord.AirportRecord ->
                                            onMapTapAirportSelected(detailRecord.value)
                                        is MapTapRecord.NavaidRecord ->
                                            onMapTapNavaidSelected(detailRecord.value)
                                        is MapTapRecord.AirspaceRecord ->
                                            onMapTapAirspaceSelected(detailRecord.value)
                                    }
                                },
                                onAddToRouteClicked = {
                                    onAddToRouteClicked(detailRecord.toRoutePoint())
                                },
                                showAddToRouteAction = !state.isRouteDestination(detailRecord.toRoutePoint().id),
                            )
                        }
                    } else {
                        if (mapTapLookup.isLoading) {
                            item { EmptyLine("Fetching nearby data...") }
                        }

                        mapTapLookup.errorMessage?.let { error ->
                            item {
                                Text(
                                    text = error,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.error,
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                )
                            }
                        }

                        if (mapTapLookup.airports.isNotEmpty()) {
                            item { SectionTitle(text = "Airports") }
                            items(mapTapLookup.airports, key = { "airport:${it.airport.id}" }) { airport ->
                                AirportRow(
                                    airport = airport,
                                    onClick = {
                                        onMapTapDetailRequested("airport:${airport.airport.id}")
                                    },
                                )
                            }
                        }

                        if (mapTapLookup.navaids.isNotEmpty()) {
                            item { SectionTitle(text = "Navaids") }
                            items(mapTapLookup.navaids, key = { "navaid:${it.navaid.id}" }) { navaid ->
                                NavaidRow(
                                    navaid = navaid,
                                    onClick = {
                                        onMapTapDetailRequested("navaid:${navaid.navaid.id}")
                                    },
                                )
                            }
                        }

                        if (mapTapLookup.airspaces.isNotEmpty()) {
                            item { SectionTitle(text = "Airspaces") }
                            items(mapTapLookup.airspaces, key = { "airspace:${it.id}" }) { airspace ->
                                AirspaceRow(
                                    airspace = airspace,
                                    onClick = {
                                        onMapTapDetailRequested("airspace:${airspace.id}")
                                    },
                                )
                            }
                        }

                        if (!mapTapLookup.isLoading &&
                            mapTapLookup.airports.isEmpty() &&
                            mapTapLookup.navaids.isEmpty() &&
                            mapTapLookup.airspaces.isEmpty()
                        ) {
                            item { EmptyLine("No nearby map data") }
                        }
                    }
                }
                SearchDockRoute.NavigationDetail -> Unit
            }

            item {
                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun NavigationDetailContent(
    state: SearchDockState,
    onWaypointDetailRequested: (id: String) -> Unit,
    onNavigationDetailClosed: () -> Unit,
    onAddWaypointRequested: () -> Unit,
    onEndFlightRequested: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxHeight()) {
        HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 14.dp),
        ) {
            val waypoint = state.selectedNavigationWaypoint
            if (waypoint != null) {
                NavigationWaypointDetail(
                    waypoint = waypoint,
                    onBack = onNavigationDetailClosed,
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
                state.nextWaypoint?.let { nextWaypoint ->
                    NavigationActionButton(
                        text = nextWaypoint.navigationDisplayLabel(),
                        kind = nextWaypoint.waypointKind(),
                        onClick = { onWaypointDetailRequested(nextWaypoint.id) },
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
                NavigationActionButton(
                    text = "Add waypoint",
                    kind = WaypointKind.Add,
                    onClick = onAddWaypointRequested,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onEndFlightRequested,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.error),
                ) {
                    Text("End flight")
                }
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
    }
}

@Composable
private fun NavigationWaypointDetail(
    waypoint: SearchDockRoutePoint,
    onBack: () -> Unit,
) {
    val kind = waypoint.waypointKind()
    DetailHeader(title = waypoint.navigationDisplayLabel(), onBack = onBack)
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        DetailRow(label = "Kind", value = kind.label)
        DetailRow(label = "Name", value = waypoint.title)
        DetailRow(label = "Position", value = formatCoordinateLabel(waypoint.latitude, waypoint.longitude))
    }
}

@Composable
private fun NavigationActionButton(
    text: String,
    kind: WaypointKind,
    onClick: () -> Unit,
) {
    ElevatedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = kind.icon,
                contentDescription = null,
                tint = kind.tint(MaterialTheme.colorScheme.primary),
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private enum class WaypointKind(val label: String) {
    Airport("Airport"),
    Airspace("Airspace"),
    Navaid("Navaid"),
    LonLat("Lon/lat"),
    Add("Add"),
}

private val WaypointKind.icon
    get() = when (this) {
        WaypointKind.Airport -> Icons.Filled.FlightTakeoff
        WaypointKind.Airspace -> Icons.Filled.Security
        WaypointKind.Navaid -> Icons.Filled.CellTower
        WaypointKind.LonLat -> Icons.Filled.LocationOn
        WaypointKind.Add -> Icons.Filled.AddCircle
    }

private fun WaypointKind.tint(primary: Color): Color = when (this) {
    WaypointKind.Airport -> Color(0xFF2E7D32)
    WaypointKind.Airspace -> Color(0xFFC62828)
    WaypointKind.Navaid -> primary
    WaypointKind.LonLat -> Color(0xFFF9A825)
    WaypointKind.Add -> primary
}

private fun SearchDockRoutePoint.waypointKind(): WaypointKind = when {
    id.startsWith("airport:") -> WaypointKind.Airport
    id.startsWith("airspace:") -> WaypointKind.Airspace
    id.startsWith("navaid:") -> WaypointKind.Navaid
    else -> WaypointKind.LonLat
}

private fun SearchDockRoutePoint.navigationDisplayLabel(): String {
    val code = id.substringAfterLast(':', title)
    return "$title ($code)"
}

private fun SearchDockState.isRouteDestination(id: String): Boolean {
    return routeDestinations.any { it.id == id }
}
