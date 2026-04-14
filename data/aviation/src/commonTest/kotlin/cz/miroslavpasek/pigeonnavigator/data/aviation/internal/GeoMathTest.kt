package cz.miroslavpasek.pigeonnavigator.data.aviation.internal

import cz.miroslavpasek.pigeonnavigator.domain.aviation.GeoPoint
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GeoMathTest {
    @Test
    fun distanceMeters_isZeroForEqualPoints() {
        val distance = GeoMath.distanceMeters(50.0, 14.0, 50.0, 14.0)
        assertTrue(distance < 0.001)
    }

    @Test
    fun polygonContains_returnsTrueInsideAndFalseOutside() {
        val polygon = listOf(
            GeoPoint(50.0, 14.0),
            GeoPoint(50.2, 14.0),
            GeoPoint(50.2, 14.2),
            GeoPoint(50.0, 14.2)
        )

        assertTrue(GeoMath.polygonContains(GeoPoint(50.1, 14.1), polygon))
        assertFalse(GeoMath.polygonContains(GeoPoint(49.9, 14.1), polygon))
    }
}
