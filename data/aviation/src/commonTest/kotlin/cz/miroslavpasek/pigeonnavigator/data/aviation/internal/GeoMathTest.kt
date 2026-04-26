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

    /**
     * Regression: a tap east of the polygon, but inside its latitude band,
     * was incorrectly reported as inside before the `coerceAtLeast(1e-12)`
     * bug in the slope denominator was removed.
     */
    @Test
    fun polygonContains_falseForPointEastOfPolygonInsideLatBand() {
        val polygon = listOf(
            GeoPoint(50.0, 14.0),
            GeoPoint(50.2, 14.0),
            GeoPoint(50.2, 14.2),
            GeoPoint(50.0, 14.2)
        )

        assertFalse(GeoMath.polygonContains(GeoPoint(50.1, 14.5), polygon))
    }

    @Test
    fun polygonContains_falseForPointWestOfPolygonInsideLatBand() {
        val polygon = listOf(
            GeoPoint(50.0, 14.0),
            GeoPoint(50.2, 14.0),
            GeoPoint(50.2, 14.2),
            GeoPoint(50.0, 14.2)
        )

        assertFalse(GeoMath.polygonContains(GeoPoint(50.1, 13.5), polygon))
    }

    @Test
    fun polygonContains_handlesTriangleWithSlopedEdges() {
        val triangle = listOf(
            GeoPoint(50.0, 14.0),
            GeoPoint(50.2, 14.0),
            GeoPoint(50.1, 14.2)
        )

        assertTrue(GeoMath.polygonContains(GeoPoint(50.1, 14.05), triangle))
        // Inside the bbox but outside the triangle's hypotenuse.
        assertFalse(GeoMath.polygonContains(GeoPoint(50.05, 14.15), triangle))
    }

    @Test
    fun polygonContains_isWindingOrderIndependent() {
        val ccw = listOf(
            GeoPoint(50.0, 14.0),
            GeoPoint(50.2, 14.0),
            GeoPoint(50.2, 14.2),
            GeoPoint(50.0, 14.2)
        )
        val cw = ccw.reversed()

        assertTrue(GeoMath.polygonContains(GeoPoint(50.1, 14.1), cw))
        assertFalse(GeoMath.polygonContains(GeoPoint(50.1, 14.5), cw))
    }

    /**
     * Concave polygon: the tap is inside the bounding box and inside the
     * convex hull, but outside the actual polygon (sits in the L's notch).
     * This is the most representative real-world failure mode for airspaces.
     */
    @Test
    fun polygonContains_falseForPointInConcavity() {
        // L-shape, vertices counter-clockwise:
        //   (50.0,14.0) - (50.0,14.3) - (50.1,14.3) - (50.1,14.1) - (50.3,14.1) - (50.3,14.0)
        val lShape = listOf(
            GeoPoint(50.0, 14.0),
            GeoPoint(50.0, 14.3),
            GeoPoint(50.1, 14.3),
            GeoPoint(50.1, 14.1),
            GeoPoint(50.3, 14.1),
            GeoPoint(50.3, 14.0)
        )

        // Inside one of the legs.
        assertTrue(GeoMath.polygonContains(GeoPoint(50.05, 14.2), lShape))
        assertTrue(GeoMath.polygonContains(GeoPoint(50.2, 14.05), lShape))

        // In the notch: inside bbox, outside polygon.
        assertFalse(GeoMath.polygonContains(GeoPoint(50.2, 14.2), lShape))
    }

    @Test
    fun polygonContains_falseForDegenerateInput() {
        assertFalse(GeoMath.polygonContains(GeoPoint(50.0, 14.0), emptyList()))
        assertFalse(
            GeoMath.polygonContains(
                GeoPoint(50.0, 14.0),
                listOf(GeoPoint(50.0, 14.0), GeoPoint(50.1, 14.1))
            )
        )
    }
}
