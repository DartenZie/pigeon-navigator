package cz.miroslavpasek.pigeonnavigator.ui.searchdock.section

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.miroslavpasek.pigeonnavigator.bridge.MapTapLookupState
import cz.miroslavpasek.pigeonnavigator.domain.aviation.Airspace
import cz.miroslavpasek.pigeonnavigator.domain.aviation.NearbyAirport
import cz.miroslavpasek.pigeonnavigator.domain.aviation.NearbyNavaid
import cz.miroslavpasek.pigeonnavigator.domain.search.SearchResult
import cz.miroslavpasek.pigeonnavigator.domain.settings.AppSettingsRepository
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockPoiItem
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockRoute
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockRoutePoint
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockState
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockHeaderMode
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import org.koin.core.context.GlobalContext

private enum class AndroidSearchDockSize { Bar, Half, Full }

private val SearchDockBarHeight = 64.dp
private val SearchDockNavigationBarHeight = 72.dp
private val SearchDockHalfHeight = 320.dp
private const val SearchDockFlingVelocityThreshold = 1600f

/**
 * Bottom-anchored search dock that animates between bar / half / full heights.
 *
 * Hosts the search-bar surface and (when expanded) the [SearchDockExpandedContent]
 * that renders nearby/search/route-planner/map-tap lists and detail panels.
 *
 * Reads search debounce settings from `AppSettingsRepository`; everything else is
 * passed in as state and callbacks.
 */
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
    onNavigationDetailRequested: () -> Unit = {},
    onNavigationWaypointDetailRequested: (id: String) -> Unit = {},
    onAddWaypointRequested: () -> Unit = {},
    onEndFlightRequested: () -> Unit = {},
    onAddToRouteClicked: (SearchDockRoutePoint) -> Unit,
    onRouteDestinationRemoved: (String) -> Unit,
    onFullExpandedChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
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

    LaunchedEffect(state.activeRoute) {
        if (state.activeRoute == SearchDockRoute.NavigationDetail && dockSize == AndroidSearchDockSize.Full) {
            dockSize = AndroidSearchDockSize.Half
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
        val allowedSize = if (state.activeRoute == SearchDockRoute.NavigationDetail && newSize == AndroidSearchDockSize.Full) {
            AndroidSearchDockSize.Half
        } else {
            newSize
        }
        dockSize = allowedSize
        val expanded = allowedSize != AndroidSearchDockSize.Bar
        if (state.isExpanded != expanded) {
            onExpandedChange(expanded)
        }
    }

    val fullHeight = maxPanelHeight.coerceAtLeast(260.dp)
    val collapsedBarHeight = if (state.headerMode == SearchDockHeaderMode.Navigation) {
        SearchDockNavigationBarHeight
    } else {
        SearchDockBarHeight
    }
    val halfHeight = SearchDockHalfHeight.coerceAtMost(fullHeight).coerceAtLeast(collapsedBarHeight)
    val targetHeight = when (dockSize) {
        AndroidSearchDockSize.Bar -> collapsedBarHeight
        AndroidSearchDockSize.Half -> halfHeight
        AndroidSearchDockSize.Full -> fullHeight
    }

    val panelHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 520f),
        label = "searchDockHeight",
    )
    val dragAdjustedPanelHeight = (
        panelHeight + with(density) { (-dragOffset * 0.25f).toDp() }
        ).coerceIn(collapsedBarHeight, fullHeight)
    val draggableState = rememberDraggableState { delta ->
        dragOffset += delta
    }

    val shape = if (dockSize == AndroidSearchDockSize.Full) {
        RoundedCornerShape(
            topStart = 22.dp,
            topEnd = 22.dp,
            bottomStart = 0.dp,
            bottomEnd = 0.dp,
        )
    } else {
        RoundedCornerShape(if (dockSize != AndroidSearchDockSize.Bar) 22.dp else 32.dp)
    }

    Surface(
        modifier = modifier
            .heightIn(min = collapsedBarHeight)
            .height(dragAdjustedPanelHeight)
            .then(
                if (dockSize == AndroidSearchDockSize.Full) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier
                        .widthIn(max = if (state.headerMode == SearchDockHeaderMode.Navigation && dockSize == AndroidSearchDockSize.Bar) 340.dp else 360.dp)
                        .fillMaxWidth()
                },
            ),
        shape = shape,
        color = colors.surfaceColorAtElevation(10.dp),
        tonalElevation = 10.dp,
        shadowElevation = 3.dp,
        border = if (dockSize == AndroidSearchDockSize.Full) null else BorderStroke(
            1.dp,
            colors.outlineVariant.copy(alpha = 0.35f),
        ),
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
                        },
                    ),
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
                                },
                            )
                        },
                    shape = RoundedCornerShape(50),
                    color = colors.outline.copy(alpha = 0.55f),
                ) {}

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .height(if (state.headerMode == SearchDockHeaderMode.Navigation) 56.dp else 44.dp)
                        .clickable {
                            if (state.headerMode == SearchDockHeaderMode.Navigation) {
                                updateDockSize(AndroidSearchDockSize.Half)
                                onNavigationDetailRequested()
                            } else {
                                updateDockSize(AndroidSearchDockSize.Full)
                                onRouteSelected(SearchDockRoute.Search)
                                focusRequester.requestFocus()
                            }
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = if (dockSize == AndroidSearchDockSize.Bar) {
                        colors.surfaceColorAtElevation(0.dp).copy(alpha = 0f)
                    } else {
                        colors.surface
                    },
                ) {
                    if (state.headerMode == SearchDockHeaderMode.Navigation) {
                        NavigationSummaryRow(state = state, modifier = Modifier.padding(horizontal = 12.dp))
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = colors.onSurface,
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
                                                color = colors.onSurfaceVariant,
                                            )
                                        }
                                        innerTextField()
                                    }
                                },
                            )
                        }
                    }
                }
            }

            if (dockSize != AndroidSearchDockSize.Bar) {
                SearchDockExpandedContent(
                    state = state,
                    mapTapLookup = mapTapLookup,
                    onNearbyPoiSelected = onNearbyPoiSelected,
                    onSearchResultSelected = onSearchResultSelected,
                    onMapTapAirportSelected = onMapTapAirportSelected,
                    onMapTapAirspaceSelected = onMapTapAirspaceSelected,
                    onMapTapNavaidSelected = onMapTapNavaidSelected,
                    onMapTapDetailRequested = onMapTapDetailRequested,
                    onMapTapDetailClosed = onMapTapDetailClosed,
                    onNavigationDetailRequested = onNavigationDetailRequested,
                    onNavigationWaypointDetailRequested = onNavigationWaypointDetailRequested,
                    onAddWaypointRequested = onAddWaypointRequested,
                    onEndFlightRequested = onEndFlightRequested,
                    onAddToRouteClicked = onAddToRouteClicked,
                    onRouteDestinationRemoved = onRouteDestinationRemoved,
                    onContentScrollStarted = {
                        if (dockSize == AndroidSearchDockSize.Half && state.activeRoute != SearchDockRoute.NavigationDetail) {
                            updateDockSize(AndroidSearchDockSize.Full)
                        }
                    },
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 10.dp)
                        .padding(top = if (state.activeRoute == SearchDockRoute.NavigationDetail) 14.dp else 0.dp),
                )
            }
        }
    }
}

@Composable
private fun NavigationSummaryRow(
    state: SearchDockState,
    modifier: Modifier = Modifier,
) {
    val summary = state.navigationSummary
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        NavigationMetric(label = "arrival", value = formatArrival(summary?.remainingSeconds))
        Spacer(modifier = Modifier.width(34.dp))
        NavigationMetric(label = "min", value = formatMinutes(summary?.remainingSeconds))
        Spacer(modifier = Modifier.width(34.dp))
        NavigationMetric(label = distanceUnit(summary?.remainingDistanceMeters), value = formatDistanceValue(summary?.remainingDistanceMeters))
    }
}

@Composable
private fun NavigationMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatArrival(remainingSeconds: Long?): String {
    val seconds = remainingSeconds ?: return "--:--"
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date(System.currentTimeMillis() + seconds * 1000L))
}

private fun formatMinutes(remainingSeconds: Long?): String {
    val seconds = remainingSeconds ?: return "--"
    return maxOf(1, (seconds / 60.0).roundToInt()).toString()
}

private fun distanceUnit(distanceMeters: Double?): String {
    val meters = distanceMeters ?: return "km"
    return if (meters >= 1000.0) "km" else "m"
}

private fun formatDistanceValue(distanceMeters: Double?): String {
    val meters = distanceMeters ?: return "--"
    return if (meters >= 1000.0) {
        ((meters / 1000.0 * 10.0).roundToInt() / 10.0).toString()
    } else {
        meters.roundToInt().toString()
    }
}
