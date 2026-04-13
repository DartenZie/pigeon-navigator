package cz.miroslavpasek.pigeonnavigator.data.terrain.internal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TerrainMathTest {

    @Test
    fun computesStableTileIdForKnownCoordinate() {
        val tileId = TerrainMath.tileId(z = 10, x = 553, y = 346)
        assertEquals(1_241_826L, tileId)
    }

    @Test
    fun computesTileCoordinateWithinTileBounds() {
        val coordinate = TerrainMath.tileCoordinate(
            latitude = 50.0755,
            longitude = 14.4378,
            zoom = 10
        )

        assertEquals(553, coordinate.xTile)
        assertEquals(346, coordinate.yTile)
        assertTrue(coordinate.pixelX in 0..255)
        assertTrue(coordinate.pixelY in 0..255)
    }

    @Test
    fun decodesTerrariumElevationMeters() {
        val level = TerrainMath.decodeTerrariumMeters(red = 128, green = 0, blue = 0)
        assertEquals(0.0, level)
    }
}
