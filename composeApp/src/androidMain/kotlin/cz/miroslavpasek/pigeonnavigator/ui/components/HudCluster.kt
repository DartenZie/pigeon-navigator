package cz.miroslavpasek.pigeonnavigator.ui.components

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
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockRoute
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockState
import kotlin.math.min

val BubbleSize = 64.dp
private val BubbleGap = 8.dp
private val ControlFadeMotion = spring<Float>(dampingRatio = 0.9f, stiffness = 420f)
private val ControlSlideMotion = spring<IntOffset>(dampingRatio = 0.9f, stiffness = 420f)

@Composable
fun HudCluster(
    speedKmh: Int,
    altitudeMeters: Int,
    mapDirection: Double,
    isRecenterVisible: Boolean,
    searchDockState: SearchDockState,
    mapTapLookup: MapTapLookupState,
    maxSearchPanelHeight: Dp,
    onCompassTap: () -> Unit,
    onRecenterTap: () -> Unit,
    onSearchDockExpandedChanged: (Boolean) -> Unit,
    onSearchDockQueryChanged: (String) -> Unit,
    onSearchDockSubmitSearch: () -> Unit,
    onSearchDockClearSearch: () -> Unit,
    onSearchDockRoutePlanningChanged: (Boolean) -> Unit,
    onSearchDockRouteSelected: (SearchDockRoute) -> Unit,
    modifier: Modifier = Modifier
) {
    val normalizedDirection = normalizeDirection(mapDirection)
    val distanceToNorth = min(normalizedDirection, 360.0 - normalizedDirection)
    val isCompassVisible = distanceToNorth > 0.05

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Column(verticalArrangement = Arrangement.spacedBy(BubbleGap)) {
                IndicatorBubble(value = altitudeMeters, unit = "m")
                IndicatorBubble(value = speedKmh, unit = "km/h")
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(verticalArrangement = Arrangement.spacedBy(BubbleGap)) {
                AnimatedVisibility(
                    visible = isRecenterVisible,
                    enter = fadeIn(animationSpec = ControlFadeMotion) +
                        slideInHorizontally(animationSpec = ControlSlideMotion) { it / 3 },
                    exit = fadeOut(animationSpec = ControlFadeMotion) +
                        slideOutHorizontally(animationSpec = ControlSlideMotion) { it / 3 }
                ) {
                    CircularActionButton(
                        onClick = onRecenterTap,
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.MyLocation,
                                contentDescription = "Recenter map"
                            )
                        }
                    )
                }

                AnimatedVisibility(
                    visible = isCompassVisible,
                    enter = fadeIn(animationSpec = ControlFadeMotion) +
                        slideInHorizontally(animationSpec = ControlSlideMotion) { it / 3 },
                    exit = fadeOut(animationSpec = ControlFadeMotion) +
                        slideOutHorizontally(animationSpec = ControlSlideMotion) { it / 3 }
                ) {
                    CircularActionButton(
                        onClick = onCompassTap,
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Explore,
                                contentDescription = "Reset north",
                                modifier = Modifier.rotate((-normalizedDirection - 45.0).toFloat())
                            )
                        }
                    )
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        )
    }
}

private fun normalizeDirection(direction: Double): Double {
    val value = direction % 360.0
    return if (value >= 0.0) value else value + 360.0
}
