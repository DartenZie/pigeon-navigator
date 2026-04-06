package cz.miroslavpasek.pigeonnavigator.ui.screens

import android.location.Location
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import cz.miroslavpasek.pigeonnavigator.data.FlightLocation
import cz.miroslavpasek.pigeonnavigator.map.MapStyleProvider
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

private const val DEFAULT_ZOOM = 10.5
private const val RECENTER_DISTANCE_METERS = 12f
private const val AWAY_FROM_USER_DISTANCE_METERS = 24f
private val PRAGUE = LatLng(50.0755, 14.4378)

@Composable
fun NavigateScreen(
    modifier: Modifier = Modifier,
    location: FlightLocation?,
    followUser: Boolean = true,
    onDirectionChange: (Double) -> Unit = {},
    onAwayFromUserLocationChange: (Boolean) -> Unit = {},
    resetNorthToken: Int = 0,
    recenterOnUserToken: Int = 0
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapStyleProvider = remember { MapStyleProvider() }

    // Initialize MapLibre once
    MapLibre.getInstance(context)

    var didSetInitialCamera by remember { mutableStateOf(false) }
    var didLoadStyle by remember { mutableStateOf(false) }
    var didAttachCameraListener by remember { mutableStateOf(false) }
    var lastResetNorthToken by remember { mutableIntStateOf(resetNorthToken) }
    var lastRecenterOnUserToken by remember { mutableIntStateOf(recenterOnUserToken) }
    var latestLocation by remember { mutableStateOf(location) }

    val styleJson = remember(mapStyleProvider) { mapStyleProvider.getStyleJson() }
    latestLocation = location

    val mapView = remember {
        MapView(context).apply {
            onCreate(null)
        }
    }

    DisposableEffect(lifecycle) {
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = mapView.onStart()
            override fun onResume(owner: LifecycleOwner) = mapView.onResume()
            override fun onPause(owner: LifecycleOwner) = mapView.onPause()
            override fun onStop(owner: LifecycleOwner) = mapView.onStop()
            override fun onDestroy(owner: LifecycleOwner) = mapView.onDestroy()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = { mv ->
            mv.getMapAsync { map ->
                if (!didLoadStyle) {
                    didLoadStyle = true
                    map.setStyle(Style.Builder().fromJson(styleJson)) {
                        map.uiSettings.isAttributionEnabled = false
                        map.uiSettings.isLogoEnabled = false
                        map.uiSettings.isCompassEnabled = false

                        if (!didSetInitialCamera) {
                            didSetInitialCamera = true
                            val target = if (location != null)
                                LatLng(location.latitude, location.longitude)
                            else
                                PRAGUE
                            map.cameraPosition = CameraPosition.Builder()
                                .target(target)
                                .zoom(DEFAULT_ZOOM)
                                .build()
                        }

                        onDirectionChange(normalizeBearing(map.cameraPosition.bearing))
                        onAwayFromUserLocationChange(
                            isAwayFromUserLocation(
                                mapTarget = map.cameraPosition.target,
                                userLocation = latestLocation
                            )
                        )
                    }
                }

                if (!didAttachCameraListener) {
                    didAttachCameraListener = true
                    map.addOnCameraMoveListener {
                        onDirectionChange(normalizeBearing(map.cameraPosition.bearing))
                        onAwayFromUserLocationChange(
                            isAwayFromUserLocation(
                                mapTarget = map.cameraPosition.target,
                                userLocation = latestLocation
                            )
                        )
                    }
                }

                if (lastResetNorthToken != resetNorthToken && map.style != null) {
                    lastResetNorthToken = resetNorthToken
                    val camera = map.cameraPosition
                    val newPosition = CameraPosition.Builder()
                        .target(camera.target)
                        .zoom(camera.zoom)
                        .tilt(camera.tilt)
                        .bearing(0.0)
                        .build()
                    map.animateCamera(
                        org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(newPosition)
                    )
                }

                if (lastRecenterOnUserToken != recenterOnUserToken && location != null && map.style != null) {
                    lastRecenterOnUserToken = recenterOnUserToken
                    val camera = map.cameraPosition
                    val target = LatLng(location.latitude, location.longitude)
                    val newPosition = CameraPosition.Builder()
                        .target(target)
                        .zoom(camera.zoom)
                        .tilt(camera.tilt)
                        .bearing(camera.bearing)
                        .build()
                    map.animateCamera(
                        org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(newPosition)
                    )
                    onAwayFromUserLocationChange(false)
                }

                onAwayFromUserLocationChange(
                    isAwayFromUserLocation(
                        mapTarget = map.cameraPosition.target,
                        userLocation = location
                    )
                )

                if (followUser && location != null && map.style != null) {
                    if (isAwayFromUserLocation(map.cameraPosition.target, location)) {
                        return@getMapAsync
                    }

                    val target = LatLng(location.latitude, location.longitude)
                    val currentTarget = map.cameraPosition.target ?: return@getMapAsync
                    val distanceBuffer = FloatArray(1)
                    Location.distanceBetween(
                        currentTarget.latitude,
                        currentTarget.longitude,
                        target.latitude,
                        target.longitude,
                        distanceBuffer
                    )

                    if (distanceBuffer[0] >= RECENTER_DISTANCE_METERS) {
                        val newPosition = CameraPosition.Builder()
                            .target(target)
                            .zoom(map.cameraPosition.zoom)
                            .bearing(map.cameraPosition.bearing)
                            .tilt(map.cameraPosition.tilt)
                            .build()
                        map.animateCamera(
                            org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(newPosition)
                        )
                    }
                }
            }
        }
    )
}

private fun normalizeBearing(rawBearing: Double): Double {
    val value = rawBearing % 360.0
    return if (value >= 0.0) value else value + 360.0
}

private fun isAwayFromUserLocation(mapTarget: LatLng?, userLocation: FlightLocation?): Boolean {
    if (mapTarget == null || userLocation == null) {
        return false
    }

    val distanceBuffer = FloatArray(1)
    Location.distanceBetween(
        mapTarget.latitude,
        mapTarget.longitude,
        userLocation.latitude,
        userLocation.longitude,
        distanceBuffer
    )
    return distanceBuffer[0] >= AWAY_FROM_USER_DISTANCE_METERS
}
