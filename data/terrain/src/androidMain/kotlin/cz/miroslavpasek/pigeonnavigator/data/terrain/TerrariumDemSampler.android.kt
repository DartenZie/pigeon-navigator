package cz.miroslavpasek.pigeonnavigator.data.terrain

import android.graphics.BitmapFactory
import cz.miroslavpasek.pigeonnavigator.core.util.result.AppResult
import cz.miroslavpasek.pigeonnavigator.domain.aviation.GetActiveMapPackageUseCase
import cz.miroslavpasek.pigeonnavigator.data.terrain.internal.PmtilesDirectory
import cz.miroslavpasek.pigeonnavigator.data.terrain.internal.TerrainMath
import cz.miroslavpasek.pigeonnavigator.domain.failure.Failure
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.File
import java.util.LinkedHashMap
import java.util.zip.GZIPInputStream
import org.koin.mp.KoinPlatform

/**
 * Creates the Android Terrarium DEM sampler backed by active OFPKG terrain PMTiles.
 */
actual fun createTerrariumDemSampler(): TerrariumDemSampler = AndroidTerrariumDemSampler()

/**
 * Samples terrain elevation by reading Terrarium tiles from active package terrain PMTiles.
 */
private class AndroidTerrariumDemSampler : TerrariumDemSampler {
    private val getActiveMapPackageUseCase: GetActiveMapPackageUseCase = KoinPlatform.getKoin().get()

    @Volatile
    private var parsedArchive: ParsedPmtilesArchive? = null

    private val decodedTileCache = object : LinkedHashMap<String, IntArray>(MAX_TILE_CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, IntArray>): Boolean {
            return size > MAX_TILE_CACHE_SIZE
        }
    }

    /**
     * Returns terrain elevation at [latitude]/[longitude], or a mapped [Failure] when unavailable.
     */
    override suspend fun sampleElevationMeters(latitude: Double, longitude: Double): AppResult<Double, Failure> {
        val archive = when (val result = getOrParseArchive()) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> return result
        }
        val coordinate = TerrainMath.tileCoordinate(latitude = latitude, longitude = longitude, zoom = archive.maxZoom)
        val tileId = TerrainMath.tileId(archive.maxZoom, coordinate.xTile, coordinate.yTile)
        val entry = archive.directory.findEntry(tileId) ?: return AppResult.Failure(Failure.OutOfCoverage)

        val cacheKey = "${archive.maxZoom}/${coordinate.xTile}/${coordinate.yTile}"
        val tilePixels = synchronized(decodedTileCache) {
            decodedTileCache[cacheKey]
        } ?: run {
            val dataStart = archive.tileDataOffset + entry.offset
            val dataEnd = dataStart + entry.length
            if (dataStart < 0 || dataEnd > archive.archiveBytes.size || dataStart >= dataEnd) {
                return diagnosticFailure("TILE_RANGE")
            }

            val rawTileBytes = archive.archiveBytes.copyOfRange(dataStart, dataEnd)
            val pngBytes = when (archive.tileCompression) {
                TILE_COMPRESSION_NONE -> rawTileBytes
                TILE_COMPRESSION_GZIP -> gunzip(rawTileBytes) ?: return diagnosticFailure("TILE_GZIP")
                else -> return diagnosticFailure("TILE_COMP_${archive.tileCompression}")
            }
            val decodedPixels = decodeTilePixels(pngBytes) ?: return diagnosticFailure("PNG_DECODE")
            synchronized(decodedTileCache) {
                decodedTileCache[cacheKey] = decodedPixels
            }
            decodedPixels
        }

        val pixelIndex = coordinate.pixelY * TILE_SIZE + coordinate.pixelX
        if (pixelIndex !in tilePixels.indices) {
            return diagnosticFailure("PIXEL_INDEX")
        }

        val pixel = tilePixels[pixelIndex]
        val red = (pixel shr 16) and 0xFF
        val green = (pixel shr 8) and 0xFF
        val blue = pixel and 0xFF

        return AppResult.Success(
            TerrainMath.decodeTerrariumMeters(red = red, green = green, blue = blue)
        )
    }

    private suspend fun getOrParseArchive(): AppResult<ParsedPmtilesArchive, Failure> {
        parsedArchive?.let { return AppResult.Success(it) }

        val activePackage = when (val result = getActiveMapPackageUseCase()) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> return diagnosticFailure("NO_ACTIVE")
        }

        val archiveBytes = runCatching {
            File(activePackage.terrainPmtilesAbsolutePath).readBytes()
        }.getOrNull() ?: return diagnosticFailure("FILE_READ")

        return synchronized(this) {
            parsedArchive?.let { return AppResult.Success(it) }
            val parsed = when (val result = parseArchive(archiveBytes)) {
                is AppResult.Success -> result.value
                is AppResult.Failure -> return result
            }
            parsedArchive = parsed
            AppResult.Success(parsed)
        }
    }

    private fun parseArchive(bytes: ByteArray): AppResult<ParsedPmtilesArchive, Failure> {
        if (bytes.size < PMTILES_HEADER_SIZE || !bytes.copyOfRange(0, 7).contentEquals(PM_TILES_MAGIC)) {
            return diagnosticFailure("BAD_HEADER")
        }

        val input = DataInputStream(ByteArrayInputStream(bytes, MAGIC_AND_VERSION_SIZE, PMTILES_HEADER_SIZE - MAGIC_AND_VERSION_SIZE))
        val rootDirOffset = input.readLittleEndianLong().toInt()
        val rootDirLength = input.readLittleEndianLong().toInt()
        input.readLittleEndianLong()
        input.readLittleEndianLong()
        input.readLittleEndianLong()
        input.readLittleEndianLong()
        val tileDataOffset = input.readLittleEndianLong().toInt()
        input.readLittleEndianLong()
        input.readLittleEndianLong()
        input.readLittleEndianLong()
        input.readLittleEndianLong()
        input.readUnsignedByte()
        val internalCompression = input.readUnsignedByte()
        val tileCompression = input.readUnsignedByte()
        input.readUnsignedByte()
        input.readUnsignedByte()
        val maxZoom = input.readUnsignedByte()

        if (rootDirOffset < 0 || rootDirLength <= 0 || rootDirOffset + rootDirLength > bytes.size) {
            return diagnosticFailure("ROOT_RANGE")
        }

        val rootDirBytes = bytes.copyOfRange(rootDirOffset, rootDirOffset + rootDirLength)
        val directoryBytes = when (internalCompression) {
            INTERNAL_COMPRESSION_NONE -> rootDirBytes
            INTERNAL_COMPRESSION_GZIP -> gunzip(rootDirBytes) ?: return diagnosticFailure("ROOT_GZIP")

            else -> return diagnosticFailure("ROOT_COMP_$internalCompression")
        }

        return runCatching {
            ParsedPmtilesArchive(
                archiveBytes = bytes,
                directory = PmtilesDirectory.decode(directoryBytes),
                tileDataOffset = tileDataOffset,
                tileCompression = tileCompression,
                maxZoom = maxZoom
            )
        }.fold(
            onSuccess = { AppResult.Success(it) },
            onFailure = { diagnosticFailure("DIR_DECODE") }
        )
    }

    private fun diagnosticFailure(code: String): AppResult.Failure<Failure> {
        return AppResult.Failure(Failure.DataUnavailableReason(code))
    }

    private fun gunzip(bytes: ByteArray): ByteArray? = runCatching {
        GZIPInputStream(ByteArrayInputStream(bytes)).use { it.readBytes() }
    }.getOrNull()

    private fun decodeTilePixels(tileBytes: ByteArray): IntArray? {
        val bitmap = BitmapFactory.decodeByteArray(tileBytes, 0, tileBytes.size) ?: return null
        if (bitmap.width != TILE_SIZE || bitmap.height != TILE_SIZE) {
            bitmap.recycle()
            return null
        }

        return try {
            IntArray(TILE_SIZE * TILE_SIZE).also { pixels ->
                bitmap.getPixels(pixels, 0, TILE_SIZE, 0, 0, TILE_SIZE, TILE_SIZE)
            }
        } finally {
            bitmap.recycle()
        }
    }

    private companion object {
        const val PMTILES_HEADER_SIZE = 127
        const val MAGIC_AND_VERSION_SIZE = 8
        const val TILE_SIZE = 256
        const val MAX_TILE_CACHE_SIZE = 48

        val PM_TILES_MAGIC = "PMTiles".encodeToByteArray()

        const val INTERNAL_COMPRESSION_NONE = 1
        const val INTERNAL_COMPRESSION_GZIP = 2
        const val TILE_COMPRESSION_NONE = 1
        const val TILE_COMPRESSION_GZIP = 2
    }
}

private data class ParsedPmtilesArchive(
    val archiveBytes: ByteArray,
    val directory: PmtilesDirectory,
    val tileDataOffset: Int,
    val tileCompression: Int,
    val maxZoom: Int
)

/**
 * Reads one little-endian unsigned 64-bit value from the current stream position.
 */
private fun DataInputStream.readLittleEndianLong(): Long {
    val bytes = ByteArray(8)
    readFully(bytes)

    return (bytes[0].toLong() and 0xFF) or
        ((bytes[1].toLong() and 0xFF) shl 8) or
        ((bytes[2].toLong() and 0xFF) shl 16) or
        ((bytes[3].toLong() and 0xFF) shl 24) or
        ((bytes[4].toLong() and 0xFF) shl 32) or
        ((bytes[5].toLong() and 0xFF) shl 40) or
        ((bytes[6].toLong() and 0xFF) shl 48) or
        ((bytes[7].toLong() and 0xFF) shl 56)
}
