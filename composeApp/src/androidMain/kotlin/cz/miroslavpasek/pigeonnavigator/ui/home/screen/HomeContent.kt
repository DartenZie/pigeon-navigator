package cz.miroslavpasek.pigeonnavigator.ui.home.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cz.miroslavpasek.pigeonnavigator.bridge.MapTapLookupState
import cz.miroslavpasek.pigeonnavigator.data.FlightLocation
import cz.miroslavpasek.pigeonnavigator.data.LocationStatus
import cz.miroslavpasek.pigeonnavigator.domain.aviation.Airspace
import cz.miroslavpasek.pigeonnavigator.domain.aviation.NearbyAirport
import cz.miroslavpasek.pigeonnavigator.domain.aviation.NearbyNavaid
import cz.miroslavpasek.pigeonnavigator.domain.search.SearchResult
import cz.miroslavpasek.pigeonnavigator.domain.terrain.TerrainHazardSample
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockMapFocus
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockPoiItem
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockRoute
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockState
import cz.miroslavpasek.pigeonnavigator.ui.components.BubbleSize
import cz.miroslavpasek.pigeonnavigator.ui.components.CircularActionButton
import cz.miroslavpasek.pigeonnavigator.ui.components.HudCluster
import cz.miroslavpasek.pigeonnavigator.ui.home.section.GpsStatusBadge
import cz.miroslavpasek.pigeonnavigator.ui.map.screen.NavigateScreen
import cz.miroslavpasek.pigeonnavigator.ui.settings.screen.SettingsScreen

/**
 * Top-level layout for the home screen.
 *
 * Stacks the moving map, GPS status badge, settings entry button, HUD cluster,
 * and the optional full-screen settings overlay. Owns no business state — the
 * hosting [AppRoot] feeds it state and callbacks.
 */
@Composable
fun HomeContent(
    location: FlightLocation?,
    locationStatus: LocationStatus?,
    terrainHazardSamples: List<TerrainHazardSample>,
    searchDockState: SearchDockState,
    mapTapLookup: MapTapLookupState,
    altitude: Int,
    altitudeUnit: String,
    speed: Int,
    speedUnit: String,
    mapDirection: Double,
    isAwayFromUserLocation: Boolean,
    isSearchDockFullExpanded: Boolean,
    isSettingsVisible: Boolean,
    resetNorthToken: Int,
    recenterOnUserToken: Int,
    mapFocus: SearchDockMapFocus?,
    mapFocusToken: Int,
    onMapDirectionChange: (Double) -> Unit,
    onMapInteraction: () -> Unit,
    onMapTap: (latitude: Double, longitude: Double) -> Unit,
    onAwayFromUserLocationChange: (Boolean) -> Unit,
    onLocationStatusBadgeClick: () -> Unit,
    onResetNorthClick: () -> Unit,
    onRecenterClick: () -> Unit,
    onOpenSettings: () -> Unit,
    onCloseSettings: () -> Unit,
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
    MaterialTheme {
        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val settingsBottomPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() +
                12.dp + BubbleSize + 12.dp
            val maxSearchPanelHeight = if (isSearchDockFullExpanded) {
                (maxHeight - settingsBottomPadding).coerceAtLeast(maxHeight * 0.72f)
            } else {
                maxHeight * 0.72f
            }

            Box(modifier = Modifier.fillMaxSize()) {
                NavigateScreen(
                    location = location,
                    terrainHazardSamples = terrainHazardSamples,
                    followUser = true,
                    onDirectionChange = onMapDirectionChange,
                    onMapInteraction = onMapInteraction,
                    onMapTap = onMapTap,
                    onAwayFromUserLocationChange = onAwayFromUserLocationChange,
                    resetNorthToken = resetNorthToken,
                    recenterOnUserToken = recenterOnUserToken,
                    mapFocus = mapFocus,
                    mapFocusToken = mapFocusToken,
                    modifier = Modifier.fillMaxSize(),
                )

                GpsStatusBadge(
                    status = locationStatus,
                    onClick = onLocationStatusBadgeClick,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 12.dp),
                )

                CircularActionButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 12.dp, end = 16.dp),
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings",
                        )
                    },
                )

                HudCluster(
                    speed = speed,
                    speedUnit = speedUnit,
                    altitude = altitude,
                    altitudeUnit = altitudeUnit,
                    mapDirection = mapDirection,
                    isRecenterVisible = isAwayFromUserLocation,
                    isSearchDockFullExpanded = isSearchDockFullExpanded,
                    searchDockState = searchDockState,
                    mapTapLookup = mapTapLookup,
                    maxSearchPanelHeight = maxSearchPanelHeight,
                    onCompassTap = onResetNorthClick,
                    onRecenterTap = onRecenterClick,
                    onSearchDockFullExpandedChanged = onSearchDockFullExpandedChanged,
                    onSearchDockExpandedChanged = onSearchDockExpandedChanged,
                    onSearchDockQueryChanged = onSearchDockQueryChanged,
                    onSearchDockSubmitSearch = onSearchDockSubmitSearch,
                    onSearchDockClearSearch = onSearchDockClearSearch,
                    onSearchDockRoutePlanningChanged = onSearchDockRoutePlanningChanged,
                    onSearchDockRouteSelected = onSearchDockRouteSelected,
                    onNearbyPoiSelected = onNearbyPoiSelected,
                    onSearchResultSelected = onSearchResultSelected,
                    onMapTapAirportSelected = onMapTapAirportSelected,
                    onMapTapAirspaceSelected = onMapTapAirspaceSelected,
                    onMapTapNavaidSelected = onMapTapNavaidSelected,
                    onMapTapDetailRequested = onMapTapDetailRequested,
                    onMapTapDetailClosed = onMapTapDetailClosed,
                    onAddToRouteClicked = onAddToRouteClicked,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(
                            horizontal = if (isSearchDockFullExpanded) 0.dp else 16.dp,
                            vertical = if (isSearchDockFullExpanded) 0.dp else 18.dp,
                        )
                        .then(if (isSearchDockFullExpanded) Modifier else Modifier.navigationBarsPadding()),
                )

                if (isSettingsVisible) {
                    SettingsScreen(
                        onClose = onCloseSettings,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}
