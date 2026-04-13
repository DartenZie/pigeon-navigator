package cz.miroslavpasek.pigeonnavigator.data.terrain.internal

import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

/**
 * Provides tile and pixel math utilities for Terrarium DEM sampling.
 */
internal object TerrainMath {
    /**
     * Returns PMTiles tile id for a Web Mercator tile coordinate.
     */
    fun tileId(z: Int, x: Int, y: Int): Long {
        val tilesBeforeZoom = ((1L shl (2 * z)) - 1L) / 3L
        return tilesBeforeZoom + hilbertIndex(z, x, y)
    }

    /**
     * Converts geographic coordinates to tile and pixel coordinates for [zoom].
     */
    fun tileCoordinate(latitude: Double, longitude: Double, zoom: Int): TileCoordinate {
        val zoomScale = 2.0.pow(zoom.toDouble())

        val xFloat = ((longitude + 180.0) / 360.0) * zoomScale
        val latitudeRadians = latitude * PI / 180.0
        val yFloat = (1.0 - ln(tan(latitudeRadians) + 1.0 / kotlin.math.cos(latitudeRadians)) / PI) / 2.0 * zoomScale

        val xTile = floor(xFloat).toInt().coerceIn(0, zoomScale.toInt() - 1)
        val yTile = floor(yFloat).toInt().coerceIn(0, zoomScale.toInt() - 1)

        val pixelX = ((xFloat - xTile) * TILE_SIZE).toInt().coerceIn(0, TILE_SIZE - 1)
        val pixelY = ((yFloat - yTile) * TILE_SIZE).toInt().coerceIn(0, TILE_SIZE - 1)

        return TileCoordinate(
            xTile = xTile,
            yTile = yTile,
            pixelX = pixelX,
            pixelY = pixelY
        )
    }

    /**
     * Decodes Terrarium RGB channels into elevation in meters.
     */
    fun decodeTerrariumMeters(red: Int, green: Int, blue: Int): Double {
        return (red * 256.0 + green + blue / 256.0) - TERRARIUM_OFFSET
    }

    private fun hilbertIndex(zoom: Int, initialX: Int, initialY: Int): Long {
        var x = initialX
        var y = initialY
        var index = 0L

        var scale = 1 shl (zoom - 1)
        while (scale > 0) {
            val rx = if ((x and scale) != 0) 1 else 0
            val ry = if ((y and scale) != 0) 1 else 0

            index += (scale.toLong() * scale.toLong()) * ((3 * rx) xor ry)
            val rotated = rotate(scale, x, y, rx, ry)
            x = rotated.first
            y = rotated.second
            scale /= 2
        }

        return index
    }

    private fun rotate(n: Int, x: Int, y: Int, rx: Int, ry: Int): Pair<Int, Int> {
        if (ry == 1) {
            return Pair(x, y)
        }

        var rotatedX = x
        var rotatedY = y

        if (rx == 1) {
            rotatedX = n - 1 - rotatedX
            rotatedY = n - 1 - rotatedY
        }

        return Pair(rotatedY, rotatedX)
    }

    private const val TILE_SIZE = 256
    private const val TERRARIUM_OFFSET = 32768.0
}

/**
 * Holds tile and pixel indices resolved from one geographic point at a specific zoom level.
 */
internal data class TileCoordinate(
    val xTile: Int,
    val yTile: Int,
    val pixelX: Int,
    val pixelY: Int
)
