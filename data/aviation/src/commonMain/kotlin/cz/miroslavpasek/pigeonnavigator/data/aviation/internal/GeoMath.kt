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

    /**
     * Canonical ray-casting point-in-polygon test (PNPOLY).
     *
     * The previous implementation clamped the latitude difference with
     * `coerceAtLeast(1e-12)`, which silently inverted the slope sign for
     * "downgoing" edges (where `previous.latitude < current.latitude`) and
     * produced wrong containment results for ~half of every polygon's edges.
     * The straddle guard already guarantees the difference is non-zero, so no
     * clamp is needed.
     *
     * Uses strict `>` on the latitude comparison: this is the canonical
     * convention that prevents double-counting when the test ray passes
     * exactly through a polygon vertex.
     */
    fun polygonContains(point: GeoPoint, polygon: List<GeoPoint>): Boolean {
        if (polygon.size < 3) return false

        var inside = false
        var previous = polygon.last()

        polygon.forEach { current ->
            val straddles =
                (current.latitude > point.latitude) != (previous.latitude > point.latitude)
            if (straddles) {
                // Non-zero by the straddle guard above.
                val dy = previous.latitude - current.latitude
                val crossingLon =
                    (previous.longitude - current.longitude) *
                        (point.latitude - current.latitude) / dy +
                        current.longitude
                if (point.longitude < crossingLon) {
                    inside = !inside
                }
            }
            previous = current
        }

        return inside
    }
}

private fun Double.toRadians(): Double = this * PI / 180.0
