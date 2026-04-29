package cz.miroslavpasek.pigeonnavigator.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cz.miroslavpasek.pigeonnavigator.bridge.MapTapLookupState
import cz.miroslavpasek.pigeonnavigator.domain.aviation.NearbyAirport
import cz.miroslavpasek.pigeonnavigator.domain.aviation.NearbyNavaid
import cz.miroslavpasek.pigeonnavigator.domain.aviation.Airspace
import cz.miroslavpasek.pigeonnavigator.domain.search.SearchResult
import cz.miroslavpasek.pigeonnavigator.domain.settings.AppSettingsRepository
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockPoiItem
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockRoute
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockRoutePoint
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockState
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.core.context.GlobalContext

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
    onAddToRouteClicked: (SearchDockRoutePoint) -> Unit,
    onRouteDestinationRemoved: (String) -> Unit,
    onFullExpandedChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var isSearchFocused by remember { mutableStateOf(false) }
    val settingsRepository = remember { GlobalContext.get().get<AppSettingsRepository>() }
    val appSettings by settingsRepository.settings.collectAsState()
    val searchPreferences = appSettings.search
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

    LaunchedEffect(dockSize) {
        onFullExpandedChange(dockSize == AndroidSearchDockSize.Full)
        if (dockSize != AndroidSearchDockSize.Full) {
            focusManager.clearFocus()
        }
    }

    LaunchedEffect(state.searchQuery, isSearchFocused, searchPreferences) {
        val trimmedQuery = state.searchQuery.trim()
        if (!isSearchFocused || trimmedQuery.length < searchPreferences.minimumQueryLength) {
            return@LaunchedEffect
        }
        delay(searchPreferences.searchDebounceMillis)
        onSubmitSearch()
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

    val shape = if (dockSize == AndroidSearchDockSize.Full) {
        androidx.compose.foundation.shape.RoundedCornerShape(
            topStart = 22.dp,
            topEnd = 22.dp,
            bottomStart = 0.dp,
            bottomEnd = 0.dp
        )
    } else {
        androidx.compose.foundation.shape.RoundedCornerShape(if (dockSize != AndroidSearchDockSize.Bar) 22.dp else 32.dp)
    }

    Surface(
        modifier = modifier
            .heightIn(min = SearchDockBarHeight)
            .height(dragAdjustedPanelHeight)
            .then(
                if (dockSize == AndroidSearchDockSize.Full) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier.widthIn(max = 360.dp)
                }
            ),
        shape = shape,
        color = colors.surfaceColorAtElevation(10.dp),
        tonalElevation = 10.dp,
        shadowElevation = 3.dp,
        border = if (dockSize == AndroidSearchDockSize.Full) null else BorderStroke(
            1.dp,
            colors.outlineVariant.copy(alpha = 0.35f)
        )
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
            ) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .width(34.dp)
                        .padding(top = 6.dp)
                        .height(4.dp)
                        .clickable {
                            updateDockSize(
                                when (dockSize) {
                                    AndroidSearchDockSize.Bar -> AndroidSearchDockSize.Half
                                    AndroidSearchDockSize.Half -> AndroidSearchDockSize.Full
                                    AndroidSearchDockSize.Full -> AndroidSearchDockSize.Bar
                                }
                            )
                        },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                    color = colors.outline.copy(alpha = 0.55f)
                ) {}

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .height(44.dp)
                        .clickable {
                            updateDockSize(AndroidSearchDockSize.Full)
                            onRouteSelected(SearchDockRoute.Search)
                            focusRequester.requestFocus()
                        },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    color = if (dockSize == AndroidSearchDockSize.Bar) {
                        colors.surfaceColorAtElevation(0.dp).copy(alpha = 0f)
                    } else {
                        colors.surface
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = colors.onSurface
                        )
                        BasicTextField(
                            value = state.searchQuery,
                            onValueChange = { query ->
                                onQueryChanged(query)
                                onRouteSelected(SearchDockRoute.Search)
                                if (query.isBlank()) {
                                    onClearSearch()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester)
                                .onFocusChanged { focusState ->
                                    isSearchFocused = focusState.isFocused
                                    if (focusState.isFocused) {
                                        updateDockSize(AndroidSearchDockSize.Full)
                                        onRouteSelected(SearchDockRoute.Search)
                                    }
                                },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.onSurface),
                            cursorBrush = SolidColor(colors.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { onSubmitSearch() }),
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (state.searchQuery.isEmpty()) {
                                        Text(
                                            text = "Search...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = colors.onSurfaceVariant
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }
                }
            }

            if (dockSize != AndroidSearchDockSize.Bar) {
                ExpandedDockContent(
                    state = state,
                    mapTapLookup = mapTapLookup,
                    onNearbyPoiSelected = onNearbyPoiSelected,
                    onSearchResultSelected = onSearchResultSelected,
                    onMapTapAirportSelected = onMapTapAirportSelected,
                    onMapTapAirspaceSelected = onMapTapAirspaceSelected,
                    onMapTapNavaidSelected = onMapTapNavaidSelected,
                    onMapTapDetailRequested = onMapTapDetailRequested,
                    onMapTapDetailClosed = onMapTapDetailClosed,
                    onAddToRouteClicked = onAddToRouteClicked,
                    onRouteDestinationRemoved = onRouteDestinationRemoved,
                    onContentScrollStarted = {
                        if (dockSize == AndroidSearchDockSize.Half) {
                            updateDockSize(AndroidSearchDockSize.Full)
                        }
                    },
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
    onNearbyPoiSelected: (SearchDockPoiItem) -> Unit,
    onSearchResultSelected: (SearchResult) -> Unit,
    onMapTapAirportSelected: (NearbyAirport) -> Unit,
    onMapTapAirspaceSelected: (Airspace) -> Unit,
    onMapTapNavaidSelected: (NearbyNavaid) -> Unit,
    onMapTapDetailRequested: (key: String) -> Unit,
    onMapTapDetailClosed: () -> Unit,
    onAddToRouteClicked: (SearchDockRoutePoint) -> Unit,
    onRouteDestinationRemoved: (String) -> Unit,
    onContentScrollStarted: () -> Unit,
    modifier: Modifier = Modifier
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

    Column(modifier = modifier) {
        HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .nestedScroll(contentScrollConnection),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            when (state.activeRoute) {
                SearchDockRoute.Nearby -> {
                    if (selectedNearbyPoi != null) {
                        item {
                            NearbyPoiDetailPanel(
                                item = selectedNearbyPoi,
                                onBack = { selectedNearbyPoiId = null },
                                onLocateClicked = { onNearbyPoiSelected(selectedNearbyPoi) },
                                onAddToRouteClicked = { onAddToRouteClicked(selectedNearbyPoi.toRoutePoint()) }
                            )
                        }
                    } else {
                        item {
                            SectionTitle(text = "Nearby Points of Interest")
                        }
                        if (state.isNearbyPoiLoading) {
                            item { EmptyLine("Fetching nearby POIs...") }
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
                            item { EmptyLine("No nearby POIs available") }
                        } else {
                            items(state.nearbyPoiItems, key = { it.id }) { item ->
                                NearbyPoiRow(
                                    item = item,
                                    onClick = { selectedNearbyPoiId = item.id }
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
                                onAddToRouteClicked = { onAddToRouteClicked(selectedSearchResult.toRoutePoint()) }
                            )
                        }
                    } else {
                        item {
                            SectionTitle(text = "Search")
                        }
                        if (state.isSearching) {
                            item { EmptyLine("Searching...") }
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
                            item { EmptyLine("No search results") }
                        } else {
                            items(state.searchResults, key = { it.id }) { result ->
                                SearchResultRow(
                                    result = result,
                                    onClick = { selectedSearchResultId = result.id }
                                )
                            }
                        }
                    }
                }

                SearchDockRoute.RoutePlanner -> {
                    item {
                        SectionTitle(text = "Route Planner")
                    }
                    if (state.routeDestinations.isEmpty()) {
                        item { EmptyLine("Add a point to start a route from your current location") }
                    } else {
                        item { EmptyLine("A Current Location") }
                        itemsIndexed(state.routeDestinations) { index, point ->
                            RouteDestinationRow(
                                label = routeLabelForIndex(index + 1),
                                point = point,
                                onRemove = { onRouteDestinationRemoved(point.id) }
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
                                        is MapTapRecord.Airport ->
                                            onMapTapAirportSelected(detailRecord.value)
                                        is MapTapRecord.Navaid ->
                                            onMapTapNavaidSelected(detailRecord.value)
                                        is MapTapRecord.Airspace ->
                                            onMapTapAirspaceSelected(detailRecord.value)
                                    }
                                },
                                onAddToRouteClicked = { onAddToRouteClicked(detailRecord.toRoutePoint()) }
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
                                    }
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
                                    }
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
                                    }
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
    onClick: () -> Unit
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
    }
}

@Composable
private fun SearchResultRow(
    result: SearchResult,
    onClick: () -> Unit
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
    }
}

@Composable
private fun AirportRow(
    airport: NearbyAirport,
    onClick: () -> Unit
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
    }
}

@Composable
private fun NavaidRow(
    navaid: NearbyNavaid,
    onClick: () -> Unit
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
    }
}

@Composable
private fun AirspaceRow(
    airspace: Airspace,
    onClick: () -> Unit
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
    }
}

@Composable
private fun DetailActionButton(
    text: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        icon()
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text)
    }
}

@Composable
private fun DetailActions(
    onLocateClicked: () -> Unit,
    onAddToRouteClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DetailActionButton(
            text = "Locate on Map",
            onClick = onLocateClicked,
            icon = {
                Icon(
                    imageVector = Icons.Filled.PinDrop,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        )
        DetailActionButton(
            text = "Add to Route",
            onClick = onAddToRouteClicked,
            icon = {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        )
    }
}

@Composable
private fun DetailHeader(title: String, onBack: () -> Unit) {
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
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DetailPanelScaffold(
    title: String,
    onBack: () -> Unit,
    rows: List<Pair<String, String>>,
    onLocateClicked: () -> Unit,
    onAddToRouteClicked: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth()) {
        DetailHeader(title = title, onBack = onBack)
        HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            rows.forEach { (label, value) ->
                DetailRow(label = label, value = value)
            }
        }
        DetailActions(
            onLocateClicked = onLocateClicked,
            onAddToRouteClicked = onAddToRouteClicked
        )
    }
}

@Composable
private fun NearbyPoiDetailPanel(
    item: SearchDockPoiItem,
    onBack: () -> Unit,
    onLocateClicked: () -> Unit,
    onAddToRouteClicked: () -> Unit
) {
    DetailPanelScaffold(
        title = item.title,
        onBack = onBack,
        rows = buildList {
            add("Kind" to item.kindLabel)
            add("Name" to item.subtitle)
            item.frequency?.takeIf { it.isNotBlank() }?.let { add("Frequency" to it) }
            add("Distance" to formatDistance(item.distanceMeters))
            add("Position" to formatCoordinateLabel(item.latitude, item.longitude))
        },
        onLocateClicked = onLocateClicked,
        onAddToRouteClicked = onAddToRouteClicked
    )
}

@Composable
private fun SearchResultDetailPanel(
    result: SearchResult,
    onBack: () -> Unit,
    onLocateClicked: () -> Unit,
    onAddToRouteClicked: () -> Unit
) {
    DetailPanelScaffold(
        title = result.title,
        onBack = onBack,
        rows = searchResultRows(result),
        onLocateClicked = onLocateClicked,
        onAddToRouteClicked = onAddToRouteClicked
    )
}

private fun searchResultRows(result: SearchResult): List<Pair<String, String>> = buildList {
    add("Kind" to result.kindLabel)
    add("Name" to result.subtitle)
    when (result) {
        is SearchResult.Airport -> add("Position" to formatCoordinateLabel(result.latitude, result.longitude))
        is SearchResult.Navaid -> {
            result.frequency?.takeIf { it.isNotBlank() }?.let { add("Frequency" to it) }
            add("Position" to formatCoordinateLabel(result.latitude, result.longitude))
        }
        is SearchResult.Airspace -> {
            add("Position" to formatCoordinateLabel(result.centerLatitude, result.centerLongitude))
            add("Bounds NE" to formatCoordinateLabel(result.bounds.maxLatitude, result.bounds.maxLongitude))
            add("Bounds SW" to formatCoordinateLabel(result.bounds.minLatitude, result.bounds.minLongitude))
        }
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
    DetailPanelScaffold(
        title = mapTapRecordTitle(record),
        onBack = onBack,
        rows = mapTapRecordRows(record),
        onLocateClicked = onLocateClicked,
        onAddToRouteClicked = onAddToRouteClicked
    )
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
            n.frequency?.takeIf { it.isNotBlank() }?.let { add("Frequency" to it) }
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

@Composable
private fun RouteDestinationRow(
    label: String,
    point: SearchDockRoutePoint,
    onRemove: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
        color = colors.surface.copy(alpha = 0.54f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier.size(28.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = colors.primary,
                contentColor = colors.onPrimary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = point.title,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRemove, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Remove route point",
                    tint = colors.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun SearchDockPoiItem.toRoutePoint(): SearchDockRoutePoint {
    return SearchDockRoutePoint(
        id = id,
        title = title,
        latitude = latitude,
        longitude = longitude
    )
}

private fun SearchResult.toRoutePoint(): SearchDockRoutePoint = when (this) {
    is SearchResult.Airport -> SearchDockRoutePoint(id, title, latitude, longitude)
    is SearchResult.Navaid -> SearchDockRoutePoint(id, title, latitude, longitude)
    is SearchResult.Airspace -> SearchDockRoutePoint(id, title, centerLatitude, centerLongitude)
}

private fun MapTapRecord.toRoutePoint(): SearchDockRoutePoint = when (this) {
    is MapTapRecord.Airport -> SearchDockRoutePoint(
        id = "airport:${value.airport.id}",
        title = value.airport.id,
        latitude = value.airport.latitude,
        longitude = value.airport.longitude
    )

    is MapTapRecord.Navaid -> SearchDockRoutePoint(
        id = "navaid:${value.navaid.id}",
        title = value.navaid.id,
        latitude = value.navaid.latitude,
        longitude = value.navaid.longitude
    )

    is MapTapRecord.Airspace -> {
        val bounds = value.points.toBounds()
        SearchDockRoutePoint(
            id = "airspace:${value.id}",
            title = value.name.ifBlank { value.id },
            latitude = bounds?.center?.latitude ?: 0.0,
            longitude = bounds?.center?.longitude ?: 0.0
        )
    }
}

private fun List<cz.miroslavpasek.pigeonnavigator.domain.aviation.GeoPoint>.toBounds(): cz.miroslavpasek.pigeonnavigator.domain.search.GeoBounds? {
    if (isEmpty()) return null
    var minLatitude = first().latitude
    var maxLatitude = first().latitude
    var minLongitude = first().longitude
    var maxLongitude = first().longitude
    for (point in drop(1)) {
        minLatitude = minOf(minLatitude, point.latitude)
        maxLatitude = maxOf(maxLatitude, point.latitude)
        minLongitude = minOf(minLongitude, point.longitude)
        maxLongitude = maxOf(maxLongitude, point.longitude)
    }
    return cz.miroslavpasek.pigeonnavigator.domain.search.GeoBounds(
        minLatitude = minLatitude,
        minLongitude = minLongitude,
        maxLatitude = maxLatitude,
        maxLongitude = maxLongitude
    )
}

private fun routeLabelForIndex(index: Int): String {
    return ('A'.code + index).toChar().toString()
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
