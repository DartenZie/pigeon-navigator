package cz.miroslavpasek.pigeonnavigator.data.terrain

import cz.miroslavpasek.pigeonnavigator.core.util.result.AppResult
import cz.miroslavpasek.pigeonnavigator.data.terrain.internal.PmtilesDirectory
import cz.miroslavpasek.pigeonnavigator.data.terrain.internal.TerrainMath
import cz.miroslavpasek.pigeonnavigator.domain.aviation.GetActiveMapPackageUseCase
import cz.miroslavpasek.pigeonnavigator.domain.failure.Failure
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGContextDrawImage
import platform.CoreGraphics.CGContextRelease
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGImageGetHeight
import platform.CoreGraphics.CGImageGetWidth
import platform.CoreGraphics.CGImageRelease
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.kCGBitmapByteOrder32Big
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.kCFAllocatorDefault
import platform.Foundation.NSFileManager
import platform.ImageIO.CGImageSourceCreateImageAtIndex
import platform.ImageIO.CGImageSourceCreateWithData
import platform.posix.memcpy
import platform.posix.memset
import platform.zlib.MAX_WBITS
import platform.zlib.Z_NO_FLUSH
import platform.zlib.Z_OK
import platform.zlib.Z_STREAM_END
import platform.zlib.inflate
import platform.zlib.inflateEnd
import platform.zlib.inflateInit2
import platform.zlib.z_stream_s
import org.koin.mp.KoinPlatform

/**
 * Creates the iOS Terrarium DEM sampler backed by active OFPKG terrain PMTiles.
 */
actual fun createTerrariumDemSampler(): TerrariumDemSampler = IosTerrariumDemSampler()

@OptIn(ExperimentalForeignApi::class)
/**
 * Samples terrain elevation by decoding Terrarium tiles from active package terrain PMTiles.
 */
private class IosTerrariumDemSampler : TerrariumDemSampler {
    private val getActiveMapPackageUseCase: GetActiveMapPackageUseCase = KoinPlatform.getKoin().get()
    private var parsedArchive: ParsedPmtilesArchive? = null

    private val decodedTileCache = mutableMapOf<String, ByteArray>()
    private val cacheOrder = mutableListOf<String>()

    /**
     * Returns terrain elevation at [latitude]/[longitude], or a mapped [Failure] when unavailable.
     */
    override suspend fun sampleElevationMeters(latitude: Double, longitude: Double): AppResult<Double, Failure> {
        val archive = getOrParseArchive() ?: return AppResult.Failure(Failure.DataUnavailable)
        val coordinate = TerrainMath.tileCoordinate(latitude = latitude, longitude = longitude, zoom = archive.maxZoom)
        val tileId = TerrainMath.tileId(archive.maxZoom, coordinate.xTile, coordinate.yTile)
        val entry = archive.directory.findEntry(tileId) ?: return AppResult.Failure(Failure.OutOfCoverage)

        val cacheKey = "${archive.maxZoom}/${coordinate.xTile}/${coordinate.yTile}"
        val rgbBytes = getCachedTile(cacheKey) ?: run {
            val dataStart = archive.tileDataOffset + entry.offset
            val dataEnd = dataStart + entry.length
            if (dataStart < 0 || dataEnd > archive.archiveBytes.size || dataStart >= dataEnd) {
                return AppResult.Failure(Failure.DataUnavailable)
            }

            val rawTileBytes = archive.archiveBytes.copyOfRange(dataStart, dataEnd)
            val tileBytes = when (archive.tileCompression) {
                TILE_COMPRESSION_NONE -> rawTileBytes
                TILE_COMPRESSION_GZIP -> gunzip(rawTileBytes) ?: return AppResult.Failure(Failure.DataUnavailable)
                else -> return AppResult.Failure(Failure.DataUnavailable)
            }

            val decoded = decodeTileRgb(tileBytes) ?: return AppResult.Failure(Failure.DataUnavailable)
            putCachedTile(cacheKey, decoded)
            decoded
        }

        val pixelIndex = (coordinate.pixelY * TILE_SIZE + coordinate.pixelX) * RGBA_STRIDE
        if (pixelIndex < 0 || pixelIndex + 2 >= rgbBytes.size) {
            return AppResult.Failure(Failure.DataUnavailable)
        }

        val red = rgbBytes[pixelIndex].toInt() and 0xFF
        val green = rgbBytes[pixelIndex + 1].toInt() and 0xFF
        val blue = rgbBytes[pixelIndex + 2].toInt() and 0xFF

        return AppResult.Success(TerrainMath.decodeTerrariumMeters(red = red, green = green, blue = blue))
    }

    private suspend fun getOrParseArchive(): ParsedPmtilesArchive? {
        parsedArchive?.let { return it }

        val bytes = readTerrainArchiveBytes() ?: return null
        val parsed = parseArchive(bytes) ?: return null
        parsedArchive = parsed
        return parsed
    }

    private fun parseArchive(bytes: ByteArray): ParsedPmtilesArchive? {
        if (bytes.size < PMTILES_HEADER_SIZE || !bytes.copyOfRange(0, 7).contentEquals(PM_TILES_MAGIC)) {
            return null
        }

        val rootDirOffset = bytes.readLittleEndianLong(8).toInt()
        val rootDirLength = bytes.readLittleEndianLong(16).toInt()
        val tileDataOffset = bytes.readLittleEndianLong(56).toInt()
        val internalCompression = bytes[97].toInt() and 0xFF
        val tileCompression = bytes[98].toInt() and 0xFF
        val maxZoom = bytes[101].toInt() and 0xFF

        if (rootDirOffset < 0 || rootDirLength <= 0 || rootDirOffset + rootDirLength > bytes.size) {
            return null
        }

        val rootDirectoryBytes = bytes.copyOfRange(rootDirOffset, rootDirOffset + rootDirLength)
        val directoryBytes = when (internalCompression) {
            INTERNAL_COMPRESSION_NONE -> rootDirectoryBytes
            INTERNAL_COMPRESSION_GZIP -> gunzip(rootDirectoryBytes) ?: return null
            else -> return null
        }

        return ParsedPmtilesArchive(
            archiveBytes = bytes,
            directory = PmtilesDirectory.decode(directoryBytes),
            tileDataOffset = tileDataOffset,
            tileCompression = tileCompression,
            maxZoom = maxZoom
        )
    }

    private suspend fun readTerrainArchiveBytes(): ByteArray? {
        val activePackage = when (val result = getActiveMapPackageUseCase()) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> return null
        }

        val filePath = activePackage.terrainPmtilesAbsolutePath
        if (!NSFileManager.defaultManager.fileExistsAtPath(filePath)) {
            return null
        }

        val data = NSFileManager.defaultManager.contentsAtPath(filePath) ?: return null
        val size = data.length.toInt()
        if (size <= 0) {
            return null
        }

        val bytes = ByteArray(size)
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), data.bytes, size.convert())
        }
        return bytes
    }

    private fun decodeTileRgb(tileBytes: ByteArray): ByteArray? {
        val data = tileBytes.toCFData() ?: return null
        val imageSource = CGImageSourceCreateWithData(data, null) ?: run {
            CFRelease(data)
            return null
        }
        val cgImage = CGImageSourceCreateImageAtIndex(imageSource, 0u, null) ?: run {
            CFRelease(imageSource)
            CFRelease(data)
            return null
        }

        val width = CGImageGetWidth(cgImage).toInt()
        val height = CGImageGetHeight(cgImage).toInt()
        if (width != TILE_SIZE || height != TILE_SIZE) {
            CGImageRelease(cgImage)
            CFRelease(imageSource)
            CFRelease(data)
            return null
        }

        val output = ByteArray(width * height * RGBA_STRIDE)
        val colorSpace = CGColorSpaceCreateDeviceRGB() ?: return null

        val context = output.usePinned { pinned ->
            CGBitmapContextCreate(
                data = pinned.addressOf(0),
                width = width.convert(),
                height = height.convert(),
                bitsPerComponent = 8.convert(),
                bytesPerRow = (width * RGBA_STRIDE).convert(),
                space = colorSpace,
                bitmapInfo = CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value or kCGBitmapByteOrder32Big
            )
        }

        if (context == null) {
            CGColorSpaceRelease(colorSpace)
            CGImageRelease(cgImage)
            CFRelease(imageSource)
            CFRelease(data)
            return null
        }

        CGContextDrawImage(
            context,
            CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble()),
            cgImage
        )
        CGContextRelease(context)
        CGColorSpaceRelease(colorSpace)
        CGImageRelease(cgImage)
        CFRelease(imageSource)
        CFRelease(data)

        return output
    }

    private fun gunzip(input: ByteArray): ByteArray? {
        if (input.isEmpty()) {
            return ByteArray(0)
        }

        return input.usePinned { pinned ->
            memScoped {
                val stream = alloc<z_stream_s>()
                memset(stream.ptr, 0, sizeOf<z_stream_s>().convert())

                stream.next_in = pinned.addressOf(0).reinterpret()
                stream.avail_in = input.size.convert()

                if (inflateInit2(stream.ptr, 16 + MAX_WBITS) != Z_OK) {
                    return@memScoped null
                }

                val chunks = mutableListOf<ByteArray>()
                var inflateStatus = Z_OK

                while (inflateStatus == Z_OK) {
                    val chunk = ByteArray(GZIP_CHUNK_SIZE)
                    inflateStatus = chunk.usePinned { outputPinned ->
                        stream.next_out = outputPinned.addressOf(0).reinterpret()
                        stream.avail_out = chunk.size.convert()
                        inflate(stream.ptr, Z_NO_FLUSH)
                    }

                    val produced = GZIP_CHUNK_SIZE - stream.avail_out.toInt()
                    if (produced > 0) {
                        chunks += chunk.copyOf(produced)
                    }
                }

                inflateEnd(stream.ptr)

                if (inflateStatus != Z_STREAM_END) {
                    return@memScoped null
                }

                val totalSize = chunks.sumOf { it.size }
                val result = ByteArray(totalSize)
                var offset = 0
                chunks.forEach { chunk ->
                    chunk.copyInto(result, destinationOffset = offset)
                    offset += chunk.size
                }
                result
            }
        }
    }

    private fun ByteArray.toCFData() =
        if (isEmpty()) {
            null
        } else usePinned { pinned ->
            CFDataCreate(
                allocator = kCFAllocatorDefault,
                bytes = pinned.addressOf(0).reinterpret(),
                length = size.convert()
            )
        }

    private fun getCachedTile(key: String): ByteArray? {
        val bytes = decodedTileCache[key] ?: return null
        cacheOrder.remove(key)
        cacheOrder.add(key)
        return bytes
    }

    private fun putCachedTile(key: String, value: ByteArray) {
        decodedTileCache[key] = value
        cacheOrder.remove(key)
        cacheOrder.add(key)

        while (cacheOrder.size > MAX_TILE_CACHE_SIZE) {
            val oldest = cacheOrder.removeAt(0)
            decodedTileCache.remove(oldest)
        }
    }

    private fun ByteArray.readLittleEndianLong(offset: Int): Long {
        return (this[offset].toLong() and 0xFFL) or
            ((this[offset + 1].toLong() and 0xFFL) shl 8) or
            ((this[offset + 2].toLong() and 0xFFL) shl 16) or
            ((this[offset + 3].toLong() and 0xFFL) shl 24) or
            ((this[offset + 4].toLong() and 0xFFL) shl 32) or
            ((this[offset + 5].toLong() and 0xFFL) shl 40) or
            ((this[offset + 6].toLong() and 0xFFL) shl 48) or
            ((this[offset + 7].toLong() and 0xFFL) shl 56)
    }

    private companion object {
        const val PMTILES_HEADER_SIZE = 127
        const val TILE_SIZE = 256
        const val RGBA_STRIDE = 4
        const val GZIP_CHUNK_SIZE = 16 * 1024
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
