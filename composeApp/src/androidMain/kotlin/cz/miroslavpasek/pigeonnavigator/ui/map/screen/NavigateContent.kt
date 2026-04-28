package cz.miroslavpasek.pigeonnavigator.ui.map.screen

import android.location.Location
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import cz.miroslavpasek.pigeonnavigator.data.FlightLocation
import cz.miroslavpasek.pigeonnavigator.domain.settings.AppSettingsRepository
import cz.miroslavpasek.pigeonnavigator.domain.terrain.TerrainHazardSample
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockMapFocus
import cz.miroslavpasek.pigeonnavigator.map.MapStyleProvider
import cz.miroslavpasek.pigeonnavigator.ui.map.internal.AIRSPACE_FIT_PADDING_PX
import cz.miroslavpasek.pigeonnavigator.ui.map.internal.PRAGUE
import cz.miroslavpasek.pigeonnavigator.ui.map.internal.RECENTER_DISTANCE_METERS
import cz.miroslavpasek.pigeonnavigator.ui.map.internal.TERRAIN_SOURCE_ID
import cz.miroslavpasek.pigeonnavigator.ui.map.internal.angularDistanceDegrees
import cz.miroslavpasek.pigeonnavigator.ui.map.internal.buildTerrainHazardFeatureCollection
import cz.miroslavpasek.pigeonnavigator.ui.map.internal.ensureTerrainHazardLayers
import cz.miroslavpasek.pigeonnavigator.ui.map.internal.ensureUserGuidanceLayers
import cz.miroslavpasek.pigeonnavigator.ui.map.internal.ensureUserLocationLayers
import cz.miroslavpasek.pigeonnavigator.ui.map.internal.normalizeBearing
import cz.miroslavpasek.pigeonnavigator.ui.map.internal.resolveDynamicDefaultZoom
import cz.miroslavpasek.pigeonnavigator.ui.map.internal.resolveTrackingBearing
import cz.miroslavpasek.pigeonnavigator.ui.map.internal.shouldShowRecenter
import cz.miroslavpasek.pigeonnavigator.ui.map.internal.updateUserGuidanceLayers
import cz.miroslavpasek.pigeonnavigator.ui.map.internal.updateUserLocationLayers
import kotlinx.coroutines.delay
import org.koin.core.context.GlobalContext
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.sources.GeoJsonSource

/**
 * Renders the moving map for the navigate screen.
 *
 * Owns the MapLibre [MapView] lifecycle, style loading, and the imperative
 * camera/tap/focus update logic. Pure helpers (geometry math, style layer
 * setup) live in `ui/map/internal/`.
 */
@Composable
fun NavigateContent(
    location: FlightLocation?,
    terrainHazardSamples: List<TerrainHazardSample>,
    followUser: Boolean,
    onDirectionChange: (Double) -> Unit,
    onMapInteraction: () -> Unit,
    onMapTap: (latitude: Double, longitude: Double) -> Unit,
    onAwayFromUserLocationChange: (Boolean) -> Unit,
    resetNorthToken: Int,
    recenterOnUserToken: Int,
    mapFocus: SearchDockMapFocus?,
    mapFocusToken: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapStyleProvider = remember { MapStyleProvider() }

    // Wait for the aviation package to be installed before building the style
    val styleJson by produceState<String?>(initialValue = null) {
        while (value == null) {
            value = mapStyleProvider.getStyleJson()
            if (value == null) delay(250)
        }
    }

    if (styleJson == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val resolvedStyleJson = styleJson!!

    // Initialize MapLibre once
    MapLibre.getInstance(context)

    var didSetInitialCamera by remember { mutableStateOf(false) }
    var didLoadStyle by remember { mutableStateOf(false) }
    var didAttachCameraListener by remember { mutableStateOf(false) }
    var didAttachMapTapListener by remember { mutableStateOf(false) }
    var lastResetNorthToken by remember { mutableIntStateOf(resetNorthToken) }
    var lastRecenterOnUserToken by remember { mutableIntStateOf(recenterOnUserToken) }
    var lastMapFocusToken by remember { mutableIntStateOf(mapFocusToken) }
    var latestLocation by remember { mutableStateOf(location) }
    var isTrackingUserLocation by remember { mutableStateOf(followUser) }
    var didCenterOnFirstGpsFix by remember { mutableStateOf(false) }
    var pendingFollowZoom by remember { mutableStateOf<Double?>(null) }

    latestLocation = location

    val settingsRepository = remember { GlobalContext.get().get<AppSettingsRepository>() }
    val appSettings by settingsRepository.settings.collectAsState()
    val mapPreferences = appSettings.map

    val dynamicDefaultZoom = resolveDynamicDefaultZoom(location, mapPreferences)

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
                    map.setStyle(Style.Builder().fromJson(resolvedStyleJson)) {
                        ensureTerrainHazardLayers(it)
                        ensureUserLocationLayers(it)
                        ensureUserGuidanceLayers(it)
                        updateUserLocationLayers(it, latestLocation)
                        updateUserGuidanceLayers(it, latestLocation)
                        map.uiSettings.isAttributionEnabled = false
                        map.uiSettings.isLogoEnabled = false
                        map.uiSettings.isCompassEnabled = false

                        if (!didSetInitialCamera) {
                            didSetInitialCamera = true
                            val target = if (location != null) {
                                didCenterOnFirstGpsFix = true
                                LatLng(location.latitude, location.longitude)
                            } else {
                                PRAGUE
                            }
                            map.cameraPosition = CameraPosition.Builder()
                                .target(target)
                                .zoom(dynamicDefaultZoom)
                                .build()
                        }

                        onDirectionChange(normalizeBearing(map.cameraPosition.bearing))
                        onAwayFromUserLocationChange(
                            shouldShowRecenter(
                                mapTarget = map.cameraPosition.target,
                                userLocation = latestLocation,
                                isTrackingUserLocation = isTrackingUserLocation,
                            ),
                        )
                    }
                }

                map.style?.let { style ->
                    ensureTerrainHazardLayers(style)
                    ensureUserLocationLayers(style)
                    ensureUserGuidanceLayers(style)
                    style.getSourceAs<GeoJsonSource>(TERRAIN_SOURCE_ID)
                        ?.setGeoJson(buildTerrainHazardFeatureCollection(terrainHazardSamples))
                    updateUserLocationLayers(style, location)
                    updateUserGuidanceLayers(style, location)
                }

                if (!didAttachCameraListener) {
                    didAttachCameraListener = true
                    map.addOnCameraMoveStartedListener { reason ->
                        if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) {
                            isTrackingUserLocation = false
                            onMapInteraction()
                        }
                    }
                    map.addOnCameraMoveListener {
                        onDirectionChange(normalizeBearing(map.cameraPosition.bearing))
                        onAwayFromUserLocationChange(
                            shouldShowRecenter(
                                mapTarget = map.cameraPosition.target,
                                userLocation = latestLocation,
                                isTrackingUserLocation = isTrackingUserLocation,
                            ),
                        )
                    }
                }

                if (!didAttachMapTapListener) {
                    didAttachMapTapListener = true
                    map.addOnMapClickListener { latLng ->
                        onMapInteraction()
                        onMapTap(latLng.latitude, latLng.longitude)
                        true
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
                    map.animateCamera(CameraUpdateFactory.newCameraPosition(newPosition))
                }

                if (lastRecenterOnUserToken != recenterOnUserToken) {
                    lastRecenterOnUserToken = recenterOnUserToken
                    isTrackingUserLocation = true
                    if (location != null && map.style != null) {
                        val camera = map.cameraPosition
                        val target = LatLng(location.latitude, location.longitude)
                        val trackingBearing = resolveTrackingBearing(camera.bearing, location)
                        pendingFollowZoom = dynamicDefaultZoom
                        val newPosition = CameraPosition.Builder()
                            .target(target)
                            .zoom(dynamicDefaultZoom)
                            .tilt(camera.tilt)
                            .bearing(trackingBearing)
                            .build()
                        map.animateCamera(CameraUpdateFactory.newCameraPosition(newPosition))
                        onAwayFromUserLocationChange(false)
                        return@getMapAsync
                    }
                }

                var recenterEvaluationTarget = map.cameraPosition.target

                val didHandleMapFocus = lastMapFocusToken != mapFocusToken
                if (didHandleMapFocus) {
                    lastMapFocusToken = mapFocusToken
                    isTrackingUserLocation = false
                    mapFocus?.let { focus ->
                        when (focus) {
                            is SearchDockMapFocus.Point -> {
                                recenterEvaluationTarget = LatLng(focus.latitude, focus.longitude)
                                val newPosition = CameraPosition.Builder()
                                    .target(recenterEvaluationTarget)
                                    .zoom(dynamicDefaultZoom)
                                    .tilt(map.cameraPosition.tilt)
                                    .bearing(map.cameraPosition.bearing)
                                    .build()
                                map.animateCamera(CameraUpdateFactory.newCameraPosition(newPosition))
                            }

                            is SearchDockMapFocus.Bounds -> {
                                recenterEvaluationTarget = LatLng(focus.centerLatitude, focus.centerLongitude)
                                val bounds = LatLngBounds.Builder()
                                    .include(LatLng(focus.bounds.minLatitude, focus.bounds.minLongitude))
                                    .include(LatLng(focus.bounds.maxLatitude, focus.bounds.maxLongitude))
                                    .build()
                                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, AIRSPACE_FIT_PADDING_PX))
                            }
                        }
                    }
                }

                if (didHandleMapFocus) {
                    onAwayFromUserLocationChange(true)
                } else {
                    onAwayFromUserLocationChange(
                        shouldShowRecenter(
                            mapTarget = recenterEvaluationTarget,
                            userLocation = location,
                            isTrackingUserLocation = isTrackingUserLocation,
                        ),
                    )
                }

                if (followUser && location != null && map.style != null) {
                    if (!didCenterOnFirstGpsFix) {
                        didCenterOnFirstGpsFix = true
                        val newPosition = CameraPosition.Builder()
                            .target(LatLng(location.latitude, location.longitude))
                            .zoom(dynamicDefaultZoom)
                            .bearing(map.cameraPosition.bearing)
                            .tilt(map.cameraPosition.tilt)
                            .build()
                        map.animateCamera(CameraUpdateFactory.newCameraPosition(newPosition))
                        onAwayFromUserLocationChange(false)
                        return@getMapAsync
                    }

                    if (!isTrackingUserLocation) {
                        return@getMapAsync
                    }

                    val target = LatLng(location.latitude, location.longitude)
                    val currentTarget = map.cameraPosition.target ?: return@getMapAsync
                    val desiredBearing = resolveTrackingBearing(map.cameraPosition.bearing, location)
                    val bearingChange = angularDistanceDegrees(map.cameraPosition.bearing, desiredBearing)
                    val distanceBuffer = FloatArray(1)
                    Location.distanceBetween(
                        currentTarget.latitude,
                        currentTarget.longitude,
                        target.latitude,
                        target.longitude,
                        distanceBuffer,
                    )

                    val shouldRecenter = distanceBuffer[0] >= RECENTER_DISTANCE_METERS
                    val shouldRotate = bearingChange >= mapPreferences.bearingUpdateThresholdDegrees
                    val followZoom = pendingFollowZoom ?: map.cameraPosition.zoom

                    if (shouldRecenter || shouldRotate) {
                        val cameraTarget = if (shouldRecenter) target else currentTarget
                        val newPosition = CameraPosition.Builder()
                            .target(cameraTarget)
                            .zoom(followZoom)
                            .bearing(desiredBearing)
                            .tilt(map.cameraPosition.tilt)
                            .build()
                        map.animateCamera(CameraUpdateFactory.newCameraPosition(newPosition))
                    }

                    if (pendingFollowZoom != null && kotlin.math.abs(map.cameraPosition.zoom - followZoom) < 0.01) {
                        pendingFollowZoom = null
                    }
                }
            }
        },
    )
}
