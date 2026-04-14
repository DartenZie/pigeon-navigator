package cz.miroslavpasek.pigeonnavigator.data.aviation.internal

import cz.miroslavpasek.pigeonnavigator.domain.aviation.GeoPoint
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal object GeoMath {
    private const val EARTH_RADIUS_METERS = 6_371_000.0
    private const val METERS_PER_LAT_DEGREE = 111_320.0

    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val latDelta = (lat2 - lat1).toRadians()
        val lonDelta = (lon2 - lon1).toRadians()

        val lat1Rad = lat1.toRadians()
        val lat2Rad = lat2.toRadians()

        val a = sin(latDelta / 2.0) * sin(latDelta / 2.0) +
            cos(lat1Rad) * cos(lat2Rad) * sin(lonDelta / 2.0) * sin(lonDelta / 2.0)

        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
        return EARTH_RADIUS_METERS * c
    }

    fun latitudeDelta(radiusMeters: Double): Double = radiusMeters / METERS_PER_LAT_DEGREE

    fun longitudeDelta(latitude: Double, radiusMeters: Double): Double {
        val latitudeCos = cos(latitude.toRadians()).coerceAtLeast(0.01)
        return radiusMeters / (METERS_PER_LAT_DEGREE * latitudeCos)
    }

    fun polygonContains(point: GeoPoint, polygon: List<GeoPoint>): Boolean {
        var contains = false
        var previous = polygon.last()

        polygon.forEach { current ->
            val intersects = (current.latitude > point.latitude) != (previous.latitude > point.latitude) &&
                point.longitude <
                ((previous.longitude - current.longitude) *
                    (point.latitude - current.latitude) /
                    ((previous.latitude - current.latitude).coerceAtLeast(1e-12)) + current.longitude)

            if (intersects) {
                contains = !contains
            }
            previous = current
        }

        return contains
    }
}

private fun Double.toRadians(): Double = this * PI / 180.0
