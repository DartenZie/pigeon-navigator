package cz.miroslavpasek.pigeonnavigator.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cz.miroslavpasek.pigeonnavigator.bridge.MapTapLookupState
import cz.miroslavpasek.pigeonnavigator.domain.aviation.Airspace
import cz.miroslavpasek.pigeonnavigator.domain.aviation.NearbyAirport
import cz.miroslavpasek.pigeonnavigator.domain.aviation.NearbyNavaid
import cz.miroslavpasek.pigeonnavigator.domain.search.SearchResult
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockPoiItem
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockRoute
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockState
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class AndroidSearchDockSize { Bar, Half, Full }

private val SearchDockBarHeight = 64.dp
private val SearchDockHalfHeight = 320.dp
private const val SearchDockFlingVelocityThreshold = 1600f

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
    onNearbyPoiSelected: (SearchDockPoiItem) -> Unit,
    onSearchResultSelected: (SearchResult) -> Unit,
    onMapTapAirportSelected: (NearbyAirport) -> Unit,
    onMapTapAirspaceSelected: (Airspace) -> Unit,
    onMapTapNavaidSelected: (NearbyNavaid) -> Unit = {},
    onMapTapDetailRequested: (key: String) -> Unit = {},
    onMapTapDetailClosed: () -> Unit = {},
    onAddToRouteClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    var dockSize by remember {
        mutableStateOf(if (state.isExpanded) AndroidSearchDockSize.Half else AndroidSearchDockSize.Bar)
    }
    var dragOffset by remember { mutableStateOf(0f) }

    LaunchedEffect(state.isExpanded) {
        when {
            !state.isExpanded && dockSize != AndroidSearchDockSize.Bar -> dockSize = AndroidSearchDockSize.Bar
            state.isExpanded && dockSize == AndroidSearchDockSize.Bar -> dockSize = AndroidSearchDockSize.Half
        }
    }

    fun updateDockSize(newSize: AndroidSearchDockSize) {
        dockSize = newSize
        val expanded = newSize != AndroidSearchDockSize.Bar
        if (state.isExpanded != expanded) {
            onExpandedChange(expanded)
        }
    }

    val fullHeight = maxPanelHeight.coerceAtLeast(260.dp)
    val halfHeight = SearchDockHalfHeight.coerceAtMost(fullHeight).coerceAtLeast(SearchDockBarHeight)
    val targetHeight = when (dockSize) {
        AndroidSearchDockSize.Bar -> SearchDockBarHeight
        AndroidSearchDockSize.Half -> halfHeight
        AndroidSearchDockSize.Full -> fullHeight
    }

    val panelHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 520f),
        label = "searchDockHeight"
    )
    val dragAdjustedPanelHeight = (
        panelHeight + with(density) { (-dragOffset * 0.25f).toDp() }
    ).coerceIn(SearchDockBarHeight, fullHeight)
    val draggableState = rememberDraggableState { delta ->
        dragOffset += delta
    }

    val title = when (state.activeRoute) {
        SearchDockRoute.Nearby -> "Nearby POIs"
        SearchDockRoute.Search -> "Search Results"
        SearchDockRoute.RoutePlanner -> "Route Planner"
        SearchDockRoute.MapTap -> "Tapped Point"
    }

    Surface(
        modifier = modifier
            .heightIn(min = SearchDockBarHeight)
            .height(dragAdjustedPanelHeight)
            .widthIn(max = 360.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(if (dockSize != AndroidSearchDockSize.Bar) 22.dp else 32.dp),
        color = colors.surfaceColorAtElevation(10.dp),
        tonalElevation = 10.dp,
        shadowElevation = 3.dp,
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .draggable(
                        state = draggableState,
                        orientation = Orientation.Vertical,
                        onDragStarted = { dragOffset = 0f },
                        onDragStopped = { velocity ->
                            val dragSlop = with(density) { 8.dp.toPx() }
                            val newSize = when {
                                velocity < -SearchDockFlingVelocityThreshold -> AndroidSearchDockSize.Full
                                velocity > SearchDockFlingVelocityThreshold -> AndroidSearchDockSize.Bar
                                abs(dragOffset) > dragSlop -> AndroidSearchDockSize.Half
                                else -> dockSize
                            }
                            dragOffset = 0f
                            updateDockSize(newSize)
                        }
                    )
                    .clickable {
                        updateDockSize(
                            if (dockSize == AndroidSearchDockSize.Bar) {
                                AndroidSearchDockSize.Half
                            } else {
                                AndroidSearchDockSize.Bar
                            }
                        )
                    }
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
                        text = if (dockSize == AndroidSearchDockSize.Bar) "Expand" else "Collapse",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.primary
                    )
                }
            }

            if (dockSize != AndroidSearchDockSize.Bar) {
                ExpandedDockContent(
                    state = state,
                    mapTapLookup = mapTapLookup,
                    onQueryChanged = onQueryChanged,
                    onSubmitSearch = onSubmitSearch,
                    onClearSearch = onClearSearch,
                    onRoutePlanningChanged = onRoutePlanningChanged,
                    onRouteSelected = onRouteSelected,
                    onNearbyPoiSelected = onNearbyPoiSelected,
                    onSearchResultSelected = onSearchResultSelected,
                    onMapTapAirportSelected = onMapTapAirportSelected,
                    onMapTapAirspaceSelected = onMapTapAirspaceSelected,
                    onMapTapNavaidSelected = onMapTapNavaidSelected,
                    onMapTapDetailRequested = onMapTapDetailRequested,
                    onMapTapDetailClosed = onMapTapDetailClosed,
                    onAddToRouteClicked = onAddToRouteClicked,
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
    onNearbyPoiSelected: (SearchDockPoiItem) -> Unit,
    onSearchResultSelected: (SearchResult) -> Unit,
    onMapTapAirportSelected: (NearbyAirport) -> Unit,
    onMapTapAirspaceSelected: (Airspace) -> Unit,
    onMapTapNavaidSelected: (NearbyNavaid) -> Unit,
    onMapTapDetailRequested: (key: String) -> Unit,
    onMapTapDetailClosed: () -> Unit,
    onAddToRouteClicked: () -> Unit,
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
                SearchDockRoute.Nearby -> {
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
                            NearbyPoiRow(
                                item = item,
                                onClick = { onNearbyPoiSelected(item) },
                                onAddToRouteClicked = onAddToRouteClicked
                            )
                        }
                    }
                }

                SearchDockRoute.Search -> {
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
                        items(state.searchResults, key = { it.id }) { result ->
                            SearchResultRow(
                                result = result,
                                onClick = { onSearchResultSelected(result) },
                                onAddToRouteClicked = onAddToRouteClicked
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
                                        is MapTapRecord.Airport ->
                                            onMapTapAirportSelected(detailRecord.value)
                                        is MapTapRecord.Navaid ->
                                            onMapTapNavaidSelected(detailRecord.value)
                                        is MapTapRecord.Airspace ->
                                            onMapTapAirspaceSelected(detailRecord.value)
                                    }
                                },
                                onAddToRouteClicked = onAddToRouteClicked
                            )
                        }
                    } else {
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

                        if (mapTapLookup.airports.isNotEmpty()) {
                            item {
                                SectionTitle(text = "Airports")
                            }
                            items(mapTapLookup.airports, key = { "airport:${it.airport.id}" }) { airport ->
                                AirportRow(
                                    airport = airport,
                                    onClick = {
                                        onMapTapDetailRequested("airport:${airport.airport.id}")
                                    },
                                    onLocateClicked = { onMapTapAirportSelected(airport) },
                                    onAddToRouteClicked = onAddToRouteClicked
                                )
                            }
                        }

                        if (mapTapLookup.navaids.isNotEmpty()) {
                            item {
                                SectionTitle(text = "Navaids")
                            }
                            items(mapTapLookup.navaids, key = { "navaid:${it.navaid.id}" }) { navaid ->
                                NavaidRow(
                                    navaid = navaid,
                                    onClick = {
                                        onMapTapDetailRequested("navaid:${navaid.navaid.id}")
                                    },
                                    onLocateClicked = { onMapTapNavaidSelected(navaid) },
                                    onAddToRouteClicked = onAddToRouteClicked
                                )
                            }
                        }

                        if (mapTapLookup.airspaces.isNotEmpty()) {
                            item {
                                SectionTitle(text = "Airspaces")
                            }
                            items(mapTapLookup.airspaces, key = { "airspace:${it.id}" }) { airspace ->
                                AirspaceRow(
                                    airspace = airspace,
                                    onClick = {
                                        onMapTapDetailRequested("airspace:${airspace.id}")
                                    },
                                    onLocateClicked = { onMapTapAirspaceSelected(airspace) },
                                    onAddToRouteClicked = onAddToRouteClicked
                                )
                            }
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
private fun NearbyPoiRow(
    item: SearchDockPoiItem,
    onClick: () -> Unit,
    onAddToRouteClicked: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
        AddToRouteButton(onClick = onAddToRouteClicked)
    }
}

@Composable
private fun SearchResultRow(
    result: SearchResult,
    onClick: () -> Unit,
    onAddToRouteClicked: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
            Text(text = result.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = "${result.kindLabel} · ${result.subtitle}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                maxLines = 1
            )
        }
        AddToRouteButton(onClick = onAddToRouteClicked)
    }
}

@Composable
private fun AirportRow(
    airport: NearbyAirport,
    onClick: () -> Unit,
    onLocateClicked: () -> Unit,
    onAddToRouteClicked: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
        LocateButton(onClick = onLocateClicked)
        AddToRouteButton(onClick = onAddToRouteClicked)
    }
}

@Composable
private fun NavaidRow(
    navaid: NearbyNavaid,
    onClick: () -> Unit,
    onLocateClicked: () -> Unit,
    onAddToRouteClicked: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
            Text(text = navaid.navaid.id, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = "${navaid.navaid.kind} · ${navaid.navaid.name}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                maxLines = 1
            )
        }
        Text(
            text = formatDistance(navaid.distanceMeters),
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant
        )
        LocateButton(onClick = onLocateClicked)
        AddToRouteButton(onClick = onAddToRouteClicked)
    }
}

@Composable
private fun AirspaceRow(
    airspace: Airspace,
    onClick: () -> Unit,
    onLocateClicked: () -> Unit,
    onAddToRouteClicked: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = airspace.name.ifBlank { airspace.id }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = "${airspace.kind} · ${formatAltitudeBand(airspace)}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
        LocateButton(onClick = onLocateClicked)
        AddToRouteButton(onClick = onAddToRouteClicked)
    }
}

@Composable
private fun AddToRouteButton(onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(32.dp)) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "Add to route",
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun LocateButton(onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(32.dp)) {
        Icon(
            imageVector = Icons.Filled.PinDrop,
            contentDescription = "Locate on map",
            modifier = Modifier.size(18.dp)
        )
    }
}

private sealed interface MapTapRecord {
    data class Airport(val value: NearbyAirport) : MapTapRecord
    data class Navaid(val value: NearbyNavaid) : MapTapRecord
    data class Airspace(val value: cz.miroslavpasek.pigeonnavigator.domain.aviation.Airspace) : MapTapRecord
}

private fun findMapTapRecord(
    key: String,
    lookup: MapTapLookupState
): MapTapRecord? {
    return when {
        key.startsWith("airport:") -> {
            val id = key.removePrefix("airport:")
            lookup.airports.firstOrNull { it.airport.id == id }?.let(MapTapRecord::Airport)
        }

        key.startsWith("navaid:") -> {
            val id = key.removePrefix("navaid:")
            lookup.navaids.firstOrNull { it.navaid.id == id }?.let(MapTapRecord::Navaid)
        }

        key.startsWith("airspace:") -> {
            val id = key.removePrefix("airspace:")
            lookup.airspaces.firstOrNull { it.id == id }?.let(MapTapRecord::Airspace)
        }

        else -> null
    }
}

@Composable
private fun MapTapDetailPanel(
    record: MapTapRecord,
    onBack: () -> Unit,
    onLocateClicked: () -> Unit,
    onAddToRouteClicked: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to results",
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = mapTapRecordTitle(record),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            LocateButton(onClick = onLocateClicked)
            AddToRouteButton(onClick = onAddToRouteClicked)
        }

        HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            mapTapRecordRows(record).forEach { (label, value) ->
                DetailRow(label = label, value = value)
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant,
            modifier = Modifier.widthIn(min = 110.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun mapTapRecordTitle(record: MapTapRecord): String = when (record) {
    is MapTapRecord.Airport -> record.value.airport.id
    is MapTapRecord.Navaid -> record.value.navaid.id
    is MapTapRecord.Airspace -> record.value.name.ifBlank { record.value.id }
}

private fun mapTapRecordRows(record: MapTapRecord): List<Pair<String, String>> = when (record) {
    is MapTapRecord.Airport -> {
        val a = record.value.airport
        buildList {
            add("Name" to a.name)
            add("Type" to a.kind)
            a.elevationMeters?.let { add("Elevation" to "${it} m") }
            add("Position" to formatCoordinateLabel(a.latitude, a.longitude))
            add("Distance" to formatDistance(record.value.distanceMeters))
        }
    }

    is MapTapRecord.Navaid -> {
        val n = record.value.navaid
        buildList {
            add("Name" to n.name)
            add("Type" to n.kind)
            if (n.detail.isNotBlank()) add("Detail" to n.detail)
            add("Position" to formatCoordinateLabel(n.latitude, n.longitude))
            add("Distance" to formatDistance(record.value.distanceMeters))
        }
    }

    is MapTapRecord.Airspace -> {
        val s = record.value
        buildList {
            if (s.name.isNotBlank()) add("Name" to s.name)
            add("Type" to s.kind)
            add("Lower" to (s.lowerLimitMeters?.let { "${it} m ${s.lowerLimitReference.orEmpty()}".trim() } ?: "SFC"))
            add("Upper" to (s.upperLimitMeters?.let { "${it} m ${s.upperLimitReference.orEmpty()}".trim() } ?: "UNL"))
            add("Vertices" to s.points.size.toString())
        }
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
    SearchDockRoute.Nearby -> "Nearby"
    SearchDockRoute.Search -> "Search"
    SearchDockRoute.RoutePlanner -> "Plan"
    SearchDockRoute.MapTap -> "Map Tap"
}
