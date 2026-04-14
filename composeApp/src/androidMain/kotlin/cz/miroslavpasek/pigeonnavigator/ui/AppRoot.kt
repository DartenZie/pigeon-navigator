package cz.miroslavpasek.pigeonnavigator.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cz.miroslavpasek.pigeonnavigator.ui.components.HudCluster
import cz.miroslavpasek.pigeonnavigator.ui.screens.NavigateScreen
import cz.miroslavpasek.pigeonnavigator.ui.viewmodel.HomeViewModel
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

@Composable
fun AppRoot(vm: HomeViewModel = koinViewModel()) {
    val state by vm.uiState.collectAsState()
    var mapDirection by remember { mutableStateOf(0.0) }
    var isAwayFromUserLocation by remember { mutableStateOf(false) }
    var resetNorthToken by remember { mutableIntStateOf(0) }
    var recenterOnUserToken by remember { mutableIntStateOf(0) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val hasLocationPermission = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (hasLocationPermission) {
            vm.refreshLocation()
        }
    }

    LaunchedEffect(state.requiresPermission) {
        if (state.requiresPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val altitudeMeters = state.location?.altitudeMeters?.roundToInt() ?: 0
    val speedKmh = state.location
        ?.speedMetersPerSecond
        ?.times(3.6f)
        ?.roundToInt()
        ?: 0

    MaterialTheme {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val maxSearchPanelHeight = maxHeight * 0.72f

            Box(modifier = Modifier.fillMaxSize()) {
                NavigateScreen(
                    location = state.location,
                    terrainHazardSamples = state.terrainHazardSamples,
                    followUser = true,
                    onDirectionChange = { mapDirection = it },
                    onMapTap = { latitude, longitude ->
                        vm.onMapTapped(latitude = latitude, longitude = longitude)
                    },
                    onAwayFromUserLocationChange = { isAwayFromUserLocation = it },
                    resetNorthToken = resetNorthToken,
                    recenterOnUserToken = recenterOnUserToken,
                    modifier = Modifier.fillMaxSize()
                )

                HudCluster(
                    speedKmh = speedKmh,
                    altitudeMeters = altitudeMeters,
                    mapDirection = mapDirection,
                    isRecenterVisible = isAwayFromUserLocation,
                    mapTapLookup = state.mapTapLookup,
                    maxSearchPanelHeight = maxSearchPanelHeight,
                    onCompassTap = { resetNorthToken += 1 },
                    onRecenterTap = { recenterOnUserToken += 1 },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 18.dp)
                        .navigationBarsPadding()
                )
            }
        }
    }
}
