package cz.miroslavpasek.pigeonnavigator.ui.home.screen

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import cz.miroslavpasek.pigeonnavigator.data.LocationStatus
import cz.miroslavpasek.pigeonnavigator.domain.settings.AppSettingsRepository
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockMapFocus
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.toMapFocus
import cz.miroslavpasek.pigeonnavigator.ui.home.internal.findActivity
import cz.miroslavpasek.pigeonnavigator.ui.home.internal.hasLocationPermission
import cz.miroslavpasek.pigeonnavigator.ui.home.internal.toMapFocus
import cz.miroslavpasek.pigeonnavigator.ui.home.model.displayLabel
import cz.miroslavpasek.pigeonnavigator.ui.home.model.toDisplayAltitude
import cz.miroslavpasek.pigeonnavigator.ui.home.model.toDisplaySpeed
import cz.miroslavpasek.pigeonnavigator.ui.viewmodel.HomeViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.context.GlobalContext

/**
 * Top-level composable for the Android app.
 *
 * Handles platform-specific lifecycle behaviour (location permission flow),
 * collects [HomeViewModel] state and app settings, and forwards everything to
 * [HomeContent], which owns the visual layout.
 */
@Composable
fun AppRoot(vm: HomeViewModel = koinViewModel()) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val state by vm.uiState.collectAsState()
    val settingsRepository = remember { GlobalContext.get().get<AppSettingsRepository>() }
    val appSettings by settingsRepository.settings.collectAsState()

    var mapDirection by remember { mutableStateOf(0.0) }
    var isAwayFromUserLocation by remember { mutableStateOf(false) }
    var resetNorthToken by remember { mutableIntStateOf(0) }
    var recenterOnUserToken by remember { mutableIntStateOf(0) }
    var isSearchDockFullExpanded by remember { mutableStateOf(false) }
    var mapFocusToken by remember { mutableIntStateOf(0) }
    var mapFocus by remember { mutableStateOf<SearchDockMapFocus?>(null) }
    var didAutoRequestLocationPermission by remember { mutableStateOf(false) }
    var didAskLocationPermission by remember { mutableStateOf(hasLocationPermission(context)) }
    var isSettingsVisible by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        didAskLocationPermission = true
        val hasLocationPermission = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (hasLocationPermission) {
            vm.refreshLocation()
        }
    }
    val requestLocationPermission = {
        val shouldOpenSettings = activity != null &&
            didAskLocationPermission &&
            !ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) &&
            !ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )

        if (shouldOpenSettings) {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null),
                ),
            )
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    LaunchedEffect(state.locationStatus) {
        if (state.locationStatus == LocationStatus.PermissionRequired && !didAutoRequestLocationPermission) {
            didAutoRequestLocationPermission = true
            requestLocationPermission()
        }
    }

    val altitude = state.location?.altitudeMeters?.toDisplayAltitude(appSettings.units.altitude) ?: 0
    val altitudeUnit = appSettings.units.altitude.displayLabel()
    val speed = state.location?.speedMetersPerSecond?.toDisplaySpeed(appSettings.units.speed) ?: 0
    val speedUnit = appSettings.units.speed.displayLabel()
    val focusMap: (SearchDockMapFocus) -> Unit = { focus ->
        mapFocus = focus
        mapFocusToken += 1
        isAwayFromUserLocation = true
        vm.collapseSearchDock()
    }

    HomeContent(
        location = state.location,
        locationStatus = state.locationStatus,
        terrainHazardSamples = state.terrainHazardSamples,
        routeDestinations = state.searchDock.routeDestinations,
        searchDockState = state.searchDock,
        mapTapLookup = state.mapTapLookup,
        altitude = altitude,
        altitudeUnit = altitudeUnit,
        speed = speed,
        speedUnit = speedUnit,
        mapDirection = mapDirection,
        isAwayFromUserLocation = isAwayFromUserLocation,
        isSearchDockFullExpanded = isSearchDockFullExpanded,
        isSettingsVisible = isSettingsVisible,
        resetNorthToken = resetNorthToken,
        recenterOnUserToken = recenterOnUserToken,
        mapFocus = mapFocus,
        mapFocusToken = mapFocusToken,
        onMapDirectionChange = { mapDirection = it },
        onMapInteraction = vm::onMapInteracted,
        onMapTap = { latitude, longitude ->
            vm.onMapTapped(latitude = latitude, longitude = longitude)
        },
        onAwayFromUserLocationChange = { isAwayFromUserLocation = it },
        onLocationStatusBadgeClick = {
            if (state.locationStatus == LocationStatus.PermissionRequired) {
                requestLocationPermission()
            }
        },
        onResetNorthClick = { resetNorthToken += 1 },
        onRecenterClick = { recenterOnUserToken += 1 },
        onOpenSettings = { isSettingsVisible = true },
        onCloseSettings = { isSettingsVisible = false },
        onSearchDockFullExpandedChanged = { isSearchDockFullExpanded = it },
        onSearchDockExpandedChanged = vm::onSearchDockExpandedChanged,
        onSearchDockQueryChanged = vm::onSearchDockQueryChanged,
        onSearchDockSubmitSearch = vm::onSearchDockSubmitSearch,
        onSearchDockClearSearch = vm::onSearchDockClearSearch,
        onSearchDockRoutePlanningChanged = vm::onSearchDockRoutePlanningChanged,
        onSearchDockRouteSelected = vm::onSearchDockRouteSelected,
        onNearbyPoiSelected = { item -> focusMap(item.toMapFocus()) },
        onSearchResultSelected = { result -> focusMap(result.toMapFocus()) },
        onMapTapAirportSelected = { airport ->
            focusMap(
                SearchDockMapFocus.Point(
                    latitude = airport.airport.latitude,
                    longitude = airport.airport.longitude,
                ),
            )
        },
        onMapTapAirspaceSelected = { airspace -> focusMap(airspace.toMapFocus()) },
        onMapTapNavaidSelected = { navaid ->
            focusMap(
                SearchDockMapFocus.Point(
                    latitude = navaid.navaid.latitude,
                    longitude = navaid.navaid.longitude,
                ),
            )
        },
        onMapTapDetailRequested = vm::onMapTapDetailRequested,
        onMapTapDetailClosed = vm::onMapTapDetailClosed,
        onNavigationDetailRequested = vm::onNavigationDetailRequested,
        onNavigationWaypointDetailRequested = vm::onNavigationWaypointDetailRequested,
        onAddWaypointRequested = vm::onAddWaypointRequested,
        onEndFlightRequested = vm::onEndFlightRequested,
        onAddToRouteClicked = vm::onRouteDestinationAdded,
        onRouteDestinationRemoved = vm::onRouteDestinationRemoved,
    )
}
