package cz.miroslavpasek.pigeonnavigator.ui.screens

import android.location.Location
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import cz.miroslavpasek.pigeonnavigator.domain.terrain.TerrainHazardLevel
import cz.miroslavpasek.pigeonnavigator.domain.terrain.TerrainHazardSample
import cz.miroslavpasek.pigeonnavigator.map.MapStyleProvider
import kotlinx.coroutines.delay
import org.maplibre.android.style.expressions.Expression.eq
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.expressions.Expression.literal
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleOpacity
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.fillColor
import org.maplibre.android.style.layers.PropertyFactory.fillOpacity
import org.maplibre.android.style.layers.PropertyFactory.fillOutlineColor
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon

private const val DEFAULT_ZOOM = 10.5
private const val RECENTER_DISTANCE_METERS = 12f
private const val AWAY_FROM_USER_DISTANCE_METERS = 24f
private const val BEARING_UPDATE_THRESHOLD_DEGREES = 4.0
private const val MIN_MOVEMENT_SPEED_MPS = 0.8f
private const val TERRAIN_SOURCE_ID = "terrain-hazard-source"
private const val TERRAIN_YELLOW_LAYER_ID = "terrain-hazard-near-layer"
private const val TERRAIN_RED_LAYER_ID = "terrain-hazard-conflict-layer"
private const val TERRAIN_SEVERITY_KEY = "severity"
private const val TERRAIN_SEVERITY_NEAR = "near"
private const val TERRAIN_SEVERITY_CONFLICT = "conflict"
private const val USER_LOCATION_DOT_SOURCE_ID = "user-location-dot-source"
private const val USER_LOCATION_DOT_LAYER_ID = "user-location-dot-layer"
private const val USER_LOCATION_ACCURACY_SOURCE_ID = "user-location-accuracy-source"
private const val USER_LOCATION_ACCURACY_LAYER_ID = "user-location-accuracy-layer"
private const val USER_LOCATION_DOT_COLOR = "#1E88E5"
private const val USER_LOCATION_DOT_STROKE_COLOR = "#FFFFFF"
private const val USER_LOCATION_ACCURACY_FILL_COLOR = "#42A5F5"
private const val EARTH_RADIUS_METERS = 6_371_000.0
private val PRAGUE = LatLng(50.0755, 14.4378)

@Composable
fun NavigateScreen(
    modifier: Modifier = Modifier,
    location: FlightLocation?,
    terrainHazardSamples: List<TerrainHazardSample> = emptyList(),
    followUser: Boolean = true,
    onDirectionChange: (Double) -> Unit = {},
    onMapTap: (latitude: Double, longitude: Double) -> Unit = { _, _ -> },
    onAwayFromUserLocationChange: (Boolean) -> Unit = {},
    resetNorthToken: Int = 0,
    recenterOnUserToken: Int = 0
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
    var latestLocation by remember { mutableStateOf(location) }
    var isTrackingUserLocation by remember { mutableStateOf(followUser) }
    var didCenterOnFirstGpsFix by remember { mutableStateOf(false) }

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
                    map.setStyle(Style.Builder().fromJson(resolvedStyleJson)) {
                        ensureTerrainHazardLayers(it)
                        ensureUserLocationLayers(it)
                        updateUserLocationLayers(it, latestLocation)
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
                                .zoom(DEFAULT_ZOOM)
                                .build()
                        }

                        onDirectionChange(normalizeBearing(map.cameraPosition.bearing))
                        onAwayFromUserLocationChange(
                            shouldShowRecenter(
                                mapTarget = map.cameraPosition.target,
                                userLocation = latestLocation,
                                isTrackingUserLocation = isTrackingUserLocation
                            )
                        )
                    }
                }

                map.style?.let { style ->
                    ensureTerrainHazardLayers(style)
                    ensureUserLocationLayers(style)
                    style.getSourceAs<GeoJsonSource>(TERRAIN_SOURCE_ID)
                        ?.setGeoJson(buildTerrainHazardFeatureCollection(terrainHazardSamples))
                    updateUserLocationLayers(style, location)
                }

                if (!didAttachCameraListener) {
                    didAttachCameraListener = true
                    map.addOnCameraMoveStartedListener { reason ->
                        if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) {
                            isTrackingUserLocation = false
                        }
                    }
                    map.addOnCameraMoveListener {
                        onDirectionChange(normalizeBearing(map.cameraPosition.bearing))
                        onAwayFromUserLocationChange(
                            shouldShowRecenter(
                                mapTarget = map.cameraPosition.target,
                                userLocation = latestLocation,
                                isTrackingUserLocation = isTrackingUserLocation
                            )
                        )
                    }
                }

                if (!didAttachMapTapListener) {
                    didAttachMapTapListener = true
                    map.addOnMapClickListener { latLng ->
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
                    map.animateCamera(
                        org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(newPosition)
                    )
                }

                if (lastRecenterOnUserToken != recenterOnUserToken) {
                    lastRecenterOnUserToken = recenterOnUserToken
                    isTrackingUserLocation = true
                    if (location != null && map.style != null) {
                        val camera = map.cameraPosition
                        val target = LatLng(location.latitude, location.longitude)
                        val trackingBearing = resolveTrackingBearing(camera.bearing, location)
                        val newPosition = CameraPosition.Builder()
                            .target(target)
                            .zoom(camera.zoom)
                            .tilt(camera.tilt)
                            .bearing(trackingBearing)
                            .build()
                        map.animateCamera(
                            org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(newPosition)
                        )
                        onAwayFromUserLocationChange(false)
                    }
                }

                onAwayFromUserLocationChange(
                    shouldShowRecenter(
                        mapTarget = map.cameraPosition.target,
                        userLocation = location,
                        isTrackingUserLocation = isTrackingUserLocation
                    )
                )

                if (followUser && location != null && map.style != null) {
                    if (!didCenterOnFirstGpsFix) {
                        didCenterOnFirstGpsFix = true
                        val newPosition = CameraPosition.Builder()
                            .target(LatLng(location.latitude, location.longitude))
                            .zoom(map.cameraPosition.zoom)
                            .bearing(map.cameraPosition.bearing)
                            .tilt(map.cameraPosition.tilt)
                            .build()
                        map.animateCamera(
                            org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(newPosition)
                        )
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
                        distanceBuffer
                    )

                    val shouldRecenter = distanceBuffer[0] >= RECENTER_DISTANCE_METERS
                    val shouldRotate = bearingChange >= BEARING_UPDATE_THRESHOLD_DEGREES

                    if (shouldRecenter || shouldRotate) {
                        val cameraTarget = if (shouldRecenter) target else currentTarget
                        val newPosition = CameraPosition.Builder()
                            .target(cameraTarget)
                            .zoom(map.cameraPosition.zoom)
                            .bearing(desiredBearing)
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

private fun ensureUserLocationLayers(style: Style) {
    if (style.getSourceAs<GeoJsonSource>(USER_LOCATION_ACCURACY_SOURCE_ID) == null) {
        style.addSource(
            GeoJsonSource(USER_LOCATION_ACCURACY_SOURCE_ID, FeatureCollection.fromFeatures(arrayOf()))
        )
    }

    if (style.getLayer(USER_LOCATION_ACCURACY_LAYER_ID) == null) {
        style.addLayer(
            FillLayer(USER_LOCATION_ACCURACY_LAYER_ID, USER_LOCATION_ACCURACY_SOURCE_ID)
                .withProperties(
                    fillColor(USER_LOCATION_ACCURACY_FILL_COLOR),
                    fillOpacity(0.2f),
                    fillOutlineColor(USER_LOCATION_ACCURACY_FILL_COLOR)
                )
        )
    }

    if (style.getSourceAs<GeoJsonSource>(USER_LOCATION_DOT_SOURCE_ID) == null) {
        style.addSource(
            GeoJsonSource(USER_LOCATION_DOT_SOURCE_ID, FeatureCollection.fromFeatures(arrayOf()))
        )
    }

    if (style.getLayer(USER_LOCATION_DOT_LAYER_ID) == null) {
        style.addLayer(
            CircleLayer(USER_LOCATION_DOT_LAYER_ID, USER_LOCATION_DOT_SOURCE_ID)
                .withProperties(
                    circleColor(USER_LOCATION_DOT_COLOR),
                    circleRadius(7f),
                    circleOpacity(1f),
                    org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth(2f),
                    org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor(USER_LOCATION_DOT_STROKE_COLOR)
                )
        )
    }
}

private fun updateUserLocationLayers(style: Style, location: FlightLocation?) {
    style.getSourceAs<GeoJsonSource>(USER_LOCATION_DOT_SOURCE_ID)
        ?.setGeoJson(buildUserLocationDotFeatureCollection(location))
    style.getSourceAs<GeoJsonSource>(USER_LOCATION_ACCURACY_SOURCE_ID)
        ?.setGeoJson(buildUserLocationAccuracyFeatureCollection(location))
}

private fun buildUserLocationDotFeatureCollection(location: FlightLocation?): FeatureCollection {
    if (location == null || location.requiresPermission) {
        return FeatureCollection.fromFeatures(arrayOf())
    }

    return FeatureCollection.fromFeatures(
        arrayOf(
            Feature.fromGeometry(Point.fromLngLat(location.longitude, location.latitude))
        )
    )
}

private fun buildUserLocationAccuracyFeatureCollection(location: FlightLocation?): FeatureCollection {
    if (location == null || location.requiresPermission) {
        return FeatureCollection.fromFeatures(arrayOf())
    }

    val radiusMeters = location.horizontalAccuracyMeters
        ?.takeIf { it.isFinite() && it > 0.0 }
        ?: return FeatureCollection.fromFeatures(arrayOf())

    val polygon = Polygon.fromLngLats(
        listOf(buildCircleRingPoints(location.latitude, location.longitude, radiusMeters))
    )

    return FeatureCollection.fromFeatures(arrayOf(Feature.fromGeometry(polygon)))
}

private fun buildCircleRingPoints(
    latitude: Double,
    longitude: Double,
    radiusMeters: Double,
    segments: Int = 64
): List<Point> {
    val latRadians = Math.toRadians(latitude)
    val lonRadians = Math.toRadians(longitude)
    val angularDistance = radiusMeters / EARTH_RADIUS_METERS

    return (0..segments).map { step ->
        val bearing = 2.0 * Math.PI * step.toDouble() / segments.toDouble()
        val sinLat = kotlin.math.sin(latRadians)
        val cosLat = kotlin.math.cos(latRadians)
        val sinAngularDistance = kotlin.math.sin(angularDistance)
        val cosAngularDistance = kotlin.math.cos(angularDistance)

        val lat2 = kotlin.math.asin(
            sinLat * cosAngularDistance + cosLat * sinAngularDistance * kotlin.math.cos(bearing)
        )
        val lon2 = lonRadians + kotlin.math.atan2(
            kotlin.math.sin(bearing) * sinAngularDistance * cosLat,
            cosAngularDistance - sinLat * kotlin.math.sin(lat2)
        )

        Point.fromLngLat(Math.toDegrees(lon2), Math.toDegrees(lat2))
    }
}

private fun ensureTerrainHazardLayers(style: Style) {
    if (style.getSourceAs<GeoJsonSource>(TERRAIN_SOURCE_ID) == null) {
        style.addSource(GeoJsonSource(TERRAIN_SOURCE_ID, FeatureCollection.fromFeatures(arrayOf())))
    }

    if (style.getLayer(TERRAIN_YELLOW_LAYER_ID) == null) {
        style.addLayer(
            CircleLayer(TERRAIN_YELLOW_LAYER_ID, TERRAIN_SOURCE_ID)
                .withFilter(eq(get(TERRAIN_SEVERITY_KEY), literal(TERRAIN_SEVERITY_NEAR)))
                .withProperties(
                    circleColor("#FFC107"),
                    circleRadius(5f),
                    circleOpacity(0.78f)
                )
        )
    }

    if (style.getLayer(TERRAIN_RED_LAYER_ID) == null) {
        style.addLayer(
            CircleLayer(TERRAIN_RED_LAYER_ID, TERRAIN_SOURCE_ID)
                .withFilter(eq(get(TERRAIN_SEVERITY_KEY), literal(TERRAIN_SEVERITY_CONFLICT)))
                .withProperties(
                    circleColor("#E53935"),
                    circleRadius(6.5f),
                    circleOpacity(0.86f)
                )
        )
    }
}

private fun buildTerrainHazardFeatureCollection(samples: List<TerrainHazardSample>): FeatureCollection {
    val features = samples.map { sample ->
        Feature.fromGeometry(Point.fromLngLat(sample.longitude, sample.latitude)).apply {
            addStringProperty(TERRAIN_SEVERITY_KEY, sample.level.toSeverityTag())
        }
    }

    return FeatureCollection.fromFeatures(features)
}

private fun TerrainHazardLevel.toSeverityTag(): String = when (this) {
    TerrainHazardLevel.NearConflict -> TERRAIN_SEVERITY_NEAR
    TerrainHazardLevel.Conflict -> TERRAIN_SEVERITY_CONFLICT
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

private fun shouldShowRecenter(
    mapTarget: LatLng?,
    userLocation: FlightLocation?,
    isTrackingUserLocation: Boolean
): Boolean {
    if (isTrackingUserLocation) {
        return false
    }
    return isAwayFromUserLocation(mapTarget = mapTarget, userLocation = userLocation)
}

private fun resolveTrackingBearing(currentBearing: Double, location: FlightLocation): Double {
    val rawBearing = location.bearingDegrees.toDouble()
    val isBearingValid = rawBearing.isFinite() && rawBearing >= 0.0 && rawBearing <= 360.0
    val isMoving = location.speedMetersPerSecond >= MIN_MOVEMENT_SPEED_MPS
    return if (isMoving && isBearingValid) rawBearing else currentBearing
}

private fun angularDistanceDegrees(from: Double, to: Double): Double {
    val diff = kotlin.math.abs(normalizeBearing(to) - normalizeBearing(from))
    return if (diff > 180.0) 360.0 - diff else diff
}
