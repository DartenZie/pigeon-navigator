package cz.miroslavpasek.pigeonnavigator.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cz.miroslavpasek.pigeonnavigator.data.LocationStatus
import cz.miroslavpasek.pigeonnavigator.domain.aviation.Airspace
import cz.miroslavpasek.pigeonnavigator.domain.search.GeoBounds
import cz.miroslavpasek.pigeonnavigator.domain.settings.AltitudeUnit
import cz.miroslavpasek.pigeonnavigator.domain.settings.AppSettingsRepository
import cz.miroslavpasek.pigeonnavigator.domain.settings.DistanceUnit
import cz.miroslavpasek.pigeonnavigator.domain.settings.SpeedUnit
import cz.miroslavpasek.pigeonnavigator.domain.settings.WarningPreferences
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockMapFocus
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.toMapFocus
import cz.miroslavpasek.pigeonnavigator.ui.components.BubbleSize
import cz.miroslavpasek.pigeonnavigator.ui.components.HudCluster
import cz.miroslavpasek.pigeonnavigator.ui.components.CircularActionButton
import cz.miroslavpasek.pigeonnavigator.ui.screens.NavigateScreen
import cz.miroslavpasek.pigeonnavigator.ui.viewmodel.HomeViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.context.GlobalContext
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun AppRoot(vm: HomeViewModel = koinViewModel()) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val state by vm.uiState.collectAsState()
    var mapDirection by remember { mutableStateOf(0.0) }
    var isAwayFromUserLocation by remember { mutableStateOf(false) }
    var resetNorthToken by remember { mutableIntStateOf(0) }
    var recenterOnUserToken by remember { mutableIntStateOf(0) }
    var isSearchDockFullExpanded by remember { mutableStateOf(false) }
    var mapFocusToken by remember { mutableIntStateOf(0) }
    var mapFocus by remember { mutableStateOf<SearchDockMapFocus?>(null) }
    var isSettingsVisible by remember { mutableStateOf(false) }
    var didAutoRequestLocationPermission by remember { mutableStateOf(false) }
    var didAskLocationPermission by remember { mutableStateOf(hasLocationPermission(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
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
                Manifest.permission.ACCESS_FINE_LOCATION
            ) &&
            !ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )

        if (shouldOpenSettings) {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                )
            )
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(state.locationStatus) {
        if (state.locationStatus == LocationStatus.PermissionRequired && !didAutoRequestLocationPermission) {
            didAutoRequestLocationPermission = true
            requestLocationPermission()
        }
    }

    val altitudeMeters = state.location?.altitudeMeters?.roundToInt() ?: 0
    val speedKmh = state.location
        ?.speedMetersPerSecond
        ?.times(3.6f)
        ?.roundToInt()
        ?: 0
    val focusMap = { focus: SearchDockMapFocus ->
        mapFocus = focus
        mapFocusToken += 1
        isAwayFromUserLocation = true
        vm.collapseSearchDock()
    }

    MaterialTheme {
        if (isSettingsVisible) {
            AppSettingsScreen(onBack = { isSettingsVisible = false })
            return@MaterialTheme
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val settingsBottomPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() +
                12.dp + BubbleSize + 12.dp
            val maxSearchPanelHeight = if (isSearchDockFullExpanded) {
                (maxHeight - settingsBottomPadding).coerceAtLeast(maxHeight * 0.72f)
            } else {
                maxHeight * 0.72f
            }

            Box(modifier = Modifier.fillMaxSize()) {
                NavigateScreen(
                    location = state.location,
                    terrainHazardSamples = state.terrainHazardSamples,
                    routeDestinations = state.searchDock.routeDestinations,
                    followUser = true,
                    onDirectionChange = { mapDirection = it },
                    onMapInteraction = vm::onMapInteracted,
                    onMapTap = { latitude, longitude ->
                        vm.onMapTapped(latitude = latitude, longitude = longitude)
                    },
                    onAwayFromUserLocationChange = { isAwayFromUserLocation = it },
                    resetNorthToken = resetNorthToken,
                    recenterOnUserToken = recenterOnUserToken,
                    mapFocus = mapFocus,
                    mapFocusToken = mapFocusToken,
                    modifier = Modifier.fillMaxSize()
                )

                GpsStatusBadge(
                    status = state.locationStatus,
                    onClick = {
                        if (state.locationStatus == LocationStatus.PermissionRequired) {
                            requestLocationPermission()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 12.dp)
                )

                CircularActionButton(
                    onClick = { isSettingsVisible = true },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 12.dp, end = 16.dp),
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings"
                        )
                    }
                )

                HudCluster(
                    speedKmh = speedKmh,
                    altitudeMeters = altitudeMeters,
                    mapDirection = mapDirection,
                    isRecenterVisible = isAwayFromUserLocation,
                    isSearchDockFullExpanded = isSearchDockFullExpanded,
                    searchDockState = state.searchDock,
                    mapTapLookup = state.mapTapLookup,
                    maxSearchPanelHeight = maxSearchPanelHeight,
                    onCompassTap = { resetNorthToken += 1 },
                    onRecenterTap = { recenterOnUserToken += 1 },
                    onSearchDockFullExpandedChanged = { isSearchDockFullExpanded = it },
                    onSearchDockExpandedChanged = vm::onSearchDockExpandedChanged,
                    onSearchDockQueryChanged = vm::onSearchDockQueryChanged,
                    onSearchDockSubmitSearch = vm::onSearchDockSubmitSearch,
                    onSearchDockClearSearch = vm::onSearchDockClearSearch,
                    onSearchDockRoutePlanningChanged = vm::onSearchDockRoutePlanningChanged,
                    onSearchDockRouteSelected = vm::onSearchDockRouteSelected,
                    onNearbyPoiSelected = { item ->
                        focusMap(item.toMapFocus())
                    },
                    onSearchResultSelected = { result ->
                        focusMap(result.toMapFocus())
                    },
                    onMapTapAirportSelected = { airport ->
                        focusMap(
                            SearchDockMapFocus.Point(
                                latitude = airport.airport.latitude,
                                longitude = airport.airport.longitude
                            )
                        )
                    },
                    onMapTapAirspaceSelected = { airspace ->
                        focusMap(airspace.toMapFocus())
                    },
                    onMapTapNavaidSelected = { navaid ->
                        focusMap(
                            SearchDockMapFocus.Point(
                                latitude = navaid.navaid.latitude,
                                longitude = navaid.navaid.longitude
                            )
                        )
                    },
                    onMapTapDetailRequested = vm::onMapTapDetailRequested,
                    onMapTapDetailClosed = vm::onMapTapDetailClosed,
                    onAddToRouteClicked = vm::onRouteDestinationAdded,
                    onRouteDestinationRemoved = vm::onRouteDestinationRemoved,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(
                            horizontal = if (isSearchDockFullExpanded) 0.dp else 16.dp,
                            vertical = if (isSearchDockFullExpanded) 0.dp else 18.dp
                        )
                        .then(if (isSearchDockFullExpanded) Modifier else Modifier.navigationBarsPadding())
                )
            }
        }
    }
}

@Composable
private fun AppSettingsScreen(onBack: () -> Unit) {
    val repository = remember { GlobalContext.get().get<AppSettingsRepository>() }
    val settings by repository.settings.collectAsState()
    val scope = rememberCoroutineScope()

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onBack) {
                    Text("Done")
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Units",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                SettingValueRow(
                    label = "Distance",
                    value = settings.units.distance.label(),
                    onClick = {
                        scope.launch {
                            repository.updateUnits(
                                settings.units.copy(distance = settings.units.distance.next())
                            )
                        }
                    }
                )
                SettingValueRow(
                    label = "Altitude",
                    value = settings.units.altitude.label(),
                    onClick = {
                        scope.launch {
                            repository.updateUnits(
                                settings.units.copy(altitude = settings.units.altitude.next())
                            )
                        }
                    }
                )
                SettingValueRow(
                    label = "Speed",
                    value = settings.units.speed.label(),
                    onClick = {
                        scope.launch {
                            repository.updateUnits(
                                settings.units.copy(speed = settings.units.speed.next())
                            )
                        }
                    }
                )

                Text(
                    text = "Warnings",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Terrain warning")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    repository.updateWarningPreferences(
                                        WarningPreferences(
                                            timeToCollisionWarningSeconds = (
                                                settings.warning.timeToCollisionWarningSeconds - 5
                                            ).coerceAtLeast(5)
                                        )
                                    )
                                }
                            }
                        ) {
                            Text("-")
                        }
                        Text("${settings.warning.timeToCollisionWarningSeconds} s")
                        TextButton(
                            onClick = {
                                scope.launch {
                                    repository.updateWarningPreferences(
                                        WarningPreferences(
                                            timeToCollisionWarningSeconds = (
                                                settings.warning.timeToCollisionWarningSeconds + 5
                                            ).coerceAtMost(600)
                                        )
                                    )
                                }
                            }
                        ) {
                            Text("+")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingValueRow(
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        TextButton(onClick = onClick) {
            Text(value)
        }
    }
}

private fun DistanceUnit.label(): String = when (this) {
    DistanceUnit.NauticalMiles -> "Nautical miles"
    DistanceUnit.Kilometers -> "Kilometers"
    DistanceUnit.Miles -> "Miles"
}

private fun DistanceUnit.next(): DistanceUnit = when (this) {
    DistanceUnit.NauticalMiles -> DistanceUnit.Kilometers
    DistanceUnit.Kilometers -> DistanceUnit.Miles
    DistanceUnit.Miles -> DistanceUnit.NauticalMiles
}

private fun AltitudeUnit.label(): String = when (this) {
    AltitudeUnit.Feet -> "Feet"
    AltitudeUnit.Meters -> "Meters"
}

private fun AltitudeUnit.next(): AltitudeUnit = when (this) {
    AltitudeUnit.Feet -> AltitudeUnit.Meters
    AltitudeUnit.Meters -> AltitudeUnit.Feet
}

private fun SpeedUnit.label(): String = when (this) {
    SpeedUnit.Knots -> "Knots"
    SpeedUnit.KilometersPerHour -> "Kilometers per hour"
    SpeedUnit.MilesPerHour -> "Miles per hour"
    SpeedUnit.MetersPerSecond -> "Meters per second"
}

private fun SpeedUnit.next(): SpeedUnit = when (this) {
    SpeedUnit.Knots -> SpeedUnit.KilometersPerHour
    SpeedUnit.KilometersPerHour -> SpeedUnit.MilesPerHour
    SpeedUnit.MilesPerHour -> SpeedUnit.MetersPerSecond
    SpeedUnit.MetersPerSecond -> SpeedUnit.Knots
}

private fun Airspace.toMapFocus(): SearchDockMapFocus {
    val bounds = points.toBounds()
        ?: GeoBounds(
            minLatitude = 0.0,
            minLongitude = 0.0,
            maxLatitude = 0.0,
            maxLongitude = 0.0
        )
    return SearchDockMapFocus.Bounds(
        centerLatitude = bounds.center.latitude,
        centerLongitude = bounds.center.longitude,
        bounds = bounds
    )
}

private fun List<cz.miroslavpasek.pigeonnavigator.domain.aviation.GeoPoint>.toBounds(): GeoBounds? {
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
    return GeoBounds(
        minLatitude = minLatitude,
        minLongitude = minLongitude,
        maxLatitude = maxLatitude,
        maxLongitude = maxLongitude
    )
}

@Composable
private fun GpsStatusBadge(
    status: LocationStatus?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (status == null || status == LocationStatus.Active) return

    val colors = MaterialTheme.colorScheme
    val isPermissionRequired = status == LocationStatus.PermissionRequired
    val label = if (isPermissionRequired) "No location access" else "GPS signal lost"
    val icon: ImageVector = if (isPermissionRequired) Icons.Filled.LocationOff else Icons.Filled.MyLocation
    val containerColor = if (isPermissionRequired) {
        colors.errorContainer
    } else {
        colors.tertiaryContainer
    }
    val contentColor = if (isPermissionRequired) {
        colors.onErrorContainer
    } else {
        colors.onTertiaryContainer
    }

    Surface(
        modifier = modifier
            .then(if (isPermissionRequired) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(percent = 50),
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = 8.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(PaddingValues(horizontal = 14.dp, vertical = 10.dp)),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
