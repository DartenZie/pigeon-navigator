package cz.miroslavpasek.pigeonnavigator.ui.map.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cz.miroslavpasek.pigeonnavigator.data.FlightLocation
import cz.miroslavpasek.pigeonnavigator.domain.terrain.TerrainHazardSample
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockMapFocus
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockRoutePoint

/**
 * Public entry point for the moving-map screen.
 *
 * Acts as a thin wrapper around [NavigateContent]; map rendering, lifecycle
 * handling, and imperative camera updates live there. Hosting screens stay
 * responsible for collecting state and forwarding callbacks.
 */
@Composable
fun NavigateScreen(
    location: FlightLocation?,
    modifier: Modifier = Modifier,
    terrainHazardSamples: List<TerrainHazardSample> = emptyList(),
    routeDestinations: List<SearchDockRoutePoint> = emptyList(),
    followUser: Boolean = true,
    onDirectionChange: (Double) -> Unit = {},
    onMapInteraction: () -> Unit = {},
    onMapTap: (latitude: Double, longitude: Double) -> Unit = { _, _ -> },
    onAwayFromUserLocationChange: (Boolean) -> Unit = {},
    resetNorthToken: Int = 0,
    recenterOnUserToken: Int = 0,
    mapFocus: SearchDockMapFocus? = null,
    mapFocusToken: Int = 0,
) {
    NavigateContent(
        location = location,
        terrainHazardSamples = terrainHazardSamples,
        routeDestinations = routeDestinations,
        followUser = followUser,
        onDirectionChange = onDirectionChange,
        onMapInteraction = onMapInteraction,
        onMapTap = onMapTap,
        onAwayFromUserLocationChange = onAwayFromUserLocationChange,
        resetNorthToken = resetNorthToken,
        recenterOnUserToken = recenterOnUserToken,
        mapFocus = mapFocus,
        mapFocusToken = mapFocusToken,
        modifier = modifier,
    )
}
