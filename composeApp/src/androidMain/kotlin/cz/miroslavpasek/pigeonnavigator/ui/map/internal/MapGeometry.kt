package cz.miroslavpasek.pigeonnavigator.ui.map.internal

import cz.miroslavpasek.pigeonnavigator.data.FlightLocation
import org.maplibre.geojson.Point

internal const val EARTH_RADIUS_METERS = 6_371_000.0
internal const val USER_GUIDANCE_LOOKAHEAD_METERS = 20_000.0
internal const val USER_GUIDANCE_CONE_HALF_ANGLE_DEGREES = 25.0
internal const val USER_GUIDANCE_MAX_MINUTE_MARKS = 12
internal const val USER_GUIDANCE_TICK_MARK_LENGTH_METERS = 180.0

/**
 * Cached geometry derived from a [FlightLocation], used to draw the user
 * guidance line, cone, and minute marks on the map.
 */
internal data class UserGuidanceGeometry(
    val origin: Point,
    val originLatitude: Double,
    val originLongitude: Double,
    val bearingDegrees: Double,
    val speedMetersPerSecond: Double,
    val trackEndpoint: Point,
    val leftConeEndpoint: Point,
    val rightConeEndpoint: Point,
)

internal fun resolveGuidanceGeometry(location: FlightLocation?): UserGuidanceGeometry? {
    if (location == null || location.requiresPermission) {
        return null
    }

    val speed = location.speedMetersPerSecond.toDouble()
    if (!speed.isFinite() || speed < MIN_MOVEMENT_SPEED_MPS) {
        return null
    }

    val bearing = location.bearingDegrees.toDouble()
    if (!bearing.isFinite() || bearing < 0.0 || bearing > 360.0) {
        return null
    }

    val origin = Point.fromLngLat(location.longitude, location.latitude)
    val normalizedBearing = normalizeBearing(bearing)
    return UserGuidanceGeometry(
        origin = origin,
        originLatitude = location.latitude,
        originLongitude = location.longitude,
        bearingDegrees = normalizedBearing,
        speedMetersPerSecond = speed,
        trackEndpoint = destinationPoint(
            latitude = location.latitude,
            longitude = location.longitude,
            bearingDegrees = normalizedBearing,
            distanceMeters = USER_GUIDANCE_LOOKAHEAD_METERS,
        ),
        leftConeEndpoint = destinationPoint(
            latitude = location.latitude,
            longitude = location.longitude,
            bearingDegrees = normalizeBearing(normalizedBearing - USER_GUIDANCE_CONE_HALF_ANGLE_DEGREES),
            distanceMeters = USER_GUIDANCE_LOOKAHEAD_METERS,
        ),
        rightConeEndpoint = destinationPoint(
            latitude = location.latitude,
            longitude = location.longitude,
            bearingDegrees = normalizeBearing(normalizedBearing + USER_GUIDANCE_CONE_HALF_ANGLE_DEGREES),
            distanceMeters = USER_GUIDANCE_LOOKAHEAD_METERS,
        ),
    )
}

internal fun destinationPoint(
    latitude: Double,
    longitude: Double,
    bearingDegrees: Double,
    distanceMeters: Double,
): Point {
    val latRadians = Math.toRadians(latitude)
    val lonRadians = Math.toRadians(longitude)
    val angularDistance = distanceMeters / EARTH_RADIUS_METERS
    val bearingRadians = Math.toRadians(bearingDegrees)

    val sinLat = kotlin.math.sin(latRadians)
    val cosLat = kotlin.math.cos(latRadians)
    val sinAngularDistance = kotlin.math.sin(angularDistance)
    val cosAngularDistance = kotlin.math.cos(angularDistance)

    val lat2 = kotlin.math.asin(
        sinLat * cosAngularDistance + cosLat * sinAngularDistance * kotlin.math.cos(bearingRadians),
    )
    val lon2 = lonRadians + kotlin.math.atan2(
        kotlin.math.sin(bearingRadians) * sinAngularDistance * cosLat,
        cosAngularDistance - sinLat * kotlin.math.sin(lat2),
    )

    return Point.fromLngLat(normalizeLongitude(Math.toDegrees(lon2)), Math.toDegrees(lat2))
}

internal fun buildCircleRingPoints(
    latitude: Double,
    longitude: Double,
    radiusMeters: Double,
    segments: Int = 64,
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
            sinLat * cosAngularDistance + cosLat * sinAngularDistance * kotlin.math.cos(bearing),
        )
        val lon2 = lonRadians + kotlin.math.atan2(
            kotlin.math.sin(bearing) * sinAngularDistance * cosLat,
            cosAngularDistance - sinLat * kotlin.math.sin(lat2),
        )

        Point.fromLngLat(Math.toDegrees(lon2), Math.toDegrees(lat2))
    }
}
