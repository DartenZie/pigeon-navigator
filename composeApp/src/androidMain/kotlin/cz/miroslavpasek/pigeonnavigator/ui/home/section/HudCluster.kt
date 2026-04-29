package cz.miroslavpasek.pigeonnavigator.ui.home.section

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import cz.miroslavpasek.pigeonnavigator.bridge.MapTapLookupState
import cz.miroslavpasek.pigeonnavigator.domain.aviation.Airspace
import cz.miroslavpasek.pigeonnavigator.domain.aviation.NearbyAirport
import cz.miroslavpasek.pigeonnavigator.domain.aviation.NearbyNavaid
import cz.miroslavpasek.pigeonnavigator.domain.search.SearchResult
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockPoiItem
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockRoute
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockState
import cz.miroslavpasek.pigeonnavigator.ui.common.component.CircularActionButton
import cz.miroslavpasek.pigeonnavigator.ui.common.component.IndicatorBubble
import cz.miroslavpasek.pigeonnavigator.ui.searchdock.section.SearchDock
import kotlin.math.min

private val BubbleGap = 8.dp
private val ControlFadeMotion = spring<Float>(dampingRatio = 0.9f, stiffness = 420f)
private val ControlSlideMotion = spring<IntOffset>(dampingRatio = 0.9f, stiffness = 420f)

/**
 * Bottom-of-screen HUD that combines the altitude/speed bubbles, the
 * compass/recenter controls, and the search dock.
 *
 * Visibility of the recenter and compass buttons is driven by
 * [isRecenterVisible] and the heading itself; both auto-hide when the search
 * dock is fully expanded so the dock can take over the bottom area.
 */
@Composable
fun HudCluster(
    speed: Int,
    speedUnit: String,
    altitude: Int,
    altitudeUnit: String,
    mapDirection: Double,
    isRecenterVisible: Boolean,
    isSearchDockFullExpanded: Boolean,
    searchDockState: SearchDockState,
    mapTapLookup: MapTapLookupState,
    maxSearchPanelHeight: Dp,
    onCompassTap: () -> Unit,
    onRecenterTap: () -> Unit,
    onSearchDockFullExpandedChanged: (Boolean) -> Unit,
    onSearchDockExpandedChanged: (Boolean) -> Unit,
    onSearchDockQueryChanged: (String) -> Unit,
    onSearchDockSubmitSearch: () -> Unit,
    onSearchDockClearSearch: () -> Unit,
    onSearchDockRoutePlanningChanged: (Boolean) -> Unit,
    onSearchDockRouteSelected: (SearchDockRoute) -> Unit,
    onNearbyPoiSelected: (SearchDockPoiItem) -> Unit,
    onSearchResultSelected: (SearchResult) -> Unit,
    onMapTapAirportSelected: (NearbyAirport) -> Unit,
    onMapTapAirspaceSelected: (Airspace) -> Unit,
    onMapTapNavaidSelected: (NearbyNavaid) -> Unit,
    onMapTapDetailRequested: (key: String) -> Unit,
    onMapTapDetailClosed: () -> Unit,
    onAddToRouteClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val normalizedDirection = normalizeDirection(mapDirection)
    val distanceToNorth = min(normalizedDirection, 360.0 - normalizedDirection)
    val isCompassVisible = distanceToNorth > 0.05

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AnimatedVisibility(visible = !isSearchDockFullExpanded) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column(verticalArrangement = Arrangement.spacedBy(BubbleGap)) {
                    IndicatorBubble(value = altitude, unit = altitudeUnit)
                    IndicatorBubble(value = speed, unit = speedUnit)
                }

                Spacer(modifier = Modifier.weight(1f))

                Column(verticalArrangement = Arrangement.spacedBy(BubbleGap)) {
                    AnimatedVisibility(
                        visible = isRecenterVisible,
                        enter = fadeIn(animationSpec = ControlFadeMotion) +
                            slideInHorizontally(animationSpec = ControlSlideMotion) { it / 3 },
                        exit = fadeOut(animationSpec = ControlFadeMotion) +
                            slideOutHorizontally(animationSpec = ControlSlideMotion) { it / 3 },
                    ) {
                        CircularActionButton(
                            onClick = onRecenterTap,
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.MyLocation,
                                    contentDescription = "Recenter map",
                                )
                            },
                        )
                    }

                    AnimatedVisibility(
                        visible = isCompassVisible,
                        enter = fadeIn(animationSpec = ControlFadeMotion) +
                            slideInHorizontally(animationSpec = ControlSlideMotion) { it / 3 },
                        exit = fadeOut(animationSpec = ControlFadeMotion) +
                            slideOutHorizontally(animationSpec = ControlSlideMotion) { it / 3 },
                    ) {
                        CircularActionButton(
                            onClick = onCompassTap,
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.Explore,
                                    contentDescription = "Reset north",
                                    modifier = Modifier.rotate((-normalizedDirection - 45.0).toFloat()),
                                )
                            },
                        )
                    }
                }
            }
        }

        SearchDock(
            state = searchDockState,
            mapTapLookup = mapTapLookup,
            maxPanelHeight = maxSearchPanelHeight,
            onExpandedChange = onSearchDockExpandedChanged,
            onQueryChanged = onSearchDockQueryChanged,
            onSubmitSearch = onSearchDockSubmitSearch,
            onClearSearch = onSearchDockClearSearch,
            onRoutePlanningChanged = onSearchDockRoutePlanningChanged,
            onRouteSelected = onSearchDockRouteSelected,
            onNearbyPoiSelected = onNearbyPoiSelected,
            onSearchResultSelected = onSearchResultSelected,
            onMapTapAirportSelected = onMapTapAirportSelected,
            onMapTapAirspaceSelected = onMapTapAirspaceSelected,
            onMapTapNavaidSelected = onMapTapNavaidSelected,
            onMapTapDetailRequested = onMapTapDetailRequested,
            onMapTapDetailClosed = onMapTapDetailClosed,
            onAddToRouteClicked = onAddToRouteClicked,
            onFullExpandedChange = onSearchDockFullExpandedChanged,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isSearchDockFullExpanded) Modifier else Modifier.padding(horizontal = 12.dp)),
        )
    }
}

private fun normalizeDirection(direction: Double): Double {
    val value = direction % 360.0
    return if (value >= 0.0) value else value + 360.0
}
