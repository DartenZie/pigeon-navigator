package cz.miroslavpasek.pigeonnavigator.ui.map.internal

import android.location.Location
import cz.miroslavpasek.pigeonnavigator.data.FlightLocation
import cz.miroslavpasek.pigeonnavigator.domain.settings.MapPreferences
import org.maplibre.android.geometry.LatLng

internal const val DEFAULT_ZOOM = 10.5
internal const val RECENTER_DISTANCE_METERS = 12f
internal const val AWAY_FROM_USER_DISTANCE_METERS = 24f
internal const val MIN_MOVEMENT_SPEED_MPS = 0.8f
internal const val AIRSPACE_FIT_PADDING_PX = 80
internal val PRAGUE = LatLng(50.0755, 14.4378)

internal fun normalizeBearing(rawBearing: Double): Double {
    val value = rawBearing % 360.0
    return if (value >= 0.0) value else value + 360.0
}

internal fun normalizeLongitude(rawLongitude: Double): Double {
    var longitude = rawLongitude
    while (longitude > 180.0) longitude -= 360.0
    while (longitude < -180.0) longitude += 360.0
    return longitude
}

internal fun angularDistanceDegrees(from: Double, to: Double): Double {
    val diff = kotlin.math.abs(normalizeBearing(to) - normalizeBearing(from))
    return if (diff > 180.0) 360.0 - diff else diff
}

internal fun isAwayFromUserLocation(
    mapTarget: LatLng?,
    userLocation: FlightLocation?,
): Boolean {
    if (mapTarget == null || userLocation == null) {
        return false
    }
    val distanceBuffer = FloatArray(1)
    Location.distanceBetween(
        mapTarget.latitude,
        mapTarget.longitude,
        userLocation.latitude,
        userLocation.longitude,
        distanceBuffer,
    )
    return distanceBuffer[0] >= AWAY_FROM_USER_DISTANCE_METERS
}

internal fun shouldShowRecenter(
    mapTarget: LatLng?,
    userLocation: FlightLocation?,
    isTrackingUserLocation: Boolean,
): Boolean {
    if (isTrackingUserLocation) {
        return false
    }
    return isAwayFromUserLocation(mapTarget = mapTarget, userLocation = userLocation)
}

internal fun resolveTrackingBearing(currentBearing: Double, location: FlightLocation): Double {
    val rawBearing = location.bearingDegrees.toDouble()
    val isBearingValid = rawBearing.isFinite() && rawBearing >= 0.0 && rawBearing <= 360.0
    val isMoving = location.speedMetersPerSecond >= MIN_MOVEMENT_SPEED_MPS
    return if (isMoving && isBearingValid) rawBearing else currentBearing
}

internal fun resolveDynamicDefaultZoom(
    location: FlightLocation?,
    mapPreferences: MapPreferences,
): Double {
    val maxSpeedKmh = mapPreferences.maxDynamicZoomSpeedKmh
    val speedKmh = ((location?.speedMetersPerSecond ?: 0f).coerceAtLeast(0f) * 3.6).coerceAtMost(
        maxSpeedKmh,
    )
    val progress = if (maxSpeedKmh > 0.0) speedKmh / maxSpeedKmh else 0.0
    return DEFAULT_ZOOM - progress * mapPreferences.maxSpeedZoomOutDelta
}
