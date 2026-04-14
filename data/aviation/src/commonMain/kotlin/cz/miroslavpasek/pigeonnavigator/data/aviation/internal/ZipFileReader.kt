package cz.miroslavpasek.pigeonnavigator.data.aviation.internal

import cz.miroslavpasek.pigeonnavigator.data.aviation.platform.DeflateDecoder
import cz.miroslavpasek.pigeonnavigator.data.aviation.platform.PlatformFileSystem

/**
 * Describes the raw data location of a ZIP entry inside the archive file.
 */
internal data class EntryDataRange(
    /** Absolute byte offset in the archive file where the entry data begins. */
    val dataStart: Long,
    /** Byte length of the (compressed) entry data. */
    val length: Long,
    /** True when the entry is stored without compression (method 0). */
    val isStored: Boolean
)

/**
 * Minimal file-based ZIP reader for OFPKG archives.
 *
 * Reads only what is needed:
 * - A small tail to locate the End-of-Central-Directory record.
 * - The central directory itself (few KB for a typical OFPKG).
 * - Individual local-file-headers on demand to resolve data offsets.
 *
 * Large STORED entries (map.pmtiles, terrain.pmtiles) are never loaded
 * into the Kotlin heap; callers receive an [EntryDataRange] and can use
 * [PlatformFileSystem.copyFileRange] to stream them directly to disk.
 */
internal class ZipFileReader(
    private val archivePath: String,
    private val fileSize: Long,
    private val fileSystem: PlatformFileSystem,
    private val deflateDecoder: DeflateDecoder = DeflateDecoder()
) {
    private val entriesByName: Map<String, ZipEntryMeta> by lazy { parseCentralDirectory() }

    /**
     * Returns the raw data location of entry [name] in the archive file.
     * Does NOT copy any bytes of the entry data.
     */
    fun entryDataRange(name: String): EntryDataRange? {
        val meta = entriesByName[name] ?: return null

        // Read local file header to get the exact data start offset
        // (extra field length can differ from the central directory value)
        val localHeader = fileSystem.readBytesAt(
            archivePath,
            meta.localHeaderOffset.toLong(),
            LOCAL_FILE_HEADER_SIZE
        ) ?: return null

        if (localHeader.readUInt32(0) != LOCAL_FILE_HEADER_SIGNATURE) return null

        val fileNameLen = localHeader.readUInt16(26)
        val extraLen = localHeader.readUInt16(28)
        val dataStart = meta.localHeaderOffset.toLong() + LOCAL_FILE_HEADER_SIZE + fileNameLen + extraLen
        val dataEnd = dataStart + meta.compressedSize

        if (dataStart < 0 || dataEnd > fileSize) return null

        return EntryDataRange(
            dataStart = dataStart,
            length = meta.compressedSize.toLong(),
            isStored = meta.compressionMethod == COMPRESSION_STORED
        )
    }

    /**
     * Reads and decompresses (if needed) a ZIP entry into memory.
     * Use for small entries (manifest, checksums, nav XML).
     * Do NOT use for large STORED entries — use [entryDataRange] + [PlatformFileSystem.copyFileRange].
     */
    fun readEntry(name: String): ByteArray? {
        val range = entryDataRange(name) ?: return null
        val raw = fileSystem.readBytesAt(archivePath, range.dataStart, range.length.toInt()) ?: return null
        return if (range.isStored) raw else deflateDecoder.decode(raw)
    }

    // ── Central directory parsing ─────────────────────────────────────────────

    private fun parseCentralDirectory(): Map<String, ZipEntryMeta> {
        val eocdOffset = findEocdOffset() ?: return emptyMap()

        // Read the EOCD record
        val eocd = fileSystem.readBytesAt(archivePath, eocdOffset, EOCD_RECORD_SIZE) ?: return emptyMap()

        val cdSize = eocd.readUInt32(12)
        val cdOffset = eocd.readUInt32(16)

        if (cdOffset < 0 || cdSize <= 0 || cdOffset.toLong() + cdSize > fileSize) return emptyMap()

        val cd = fileSystem.readBytesAt(archivePath, cdOffset.toLong(), cdSize) ?: return emptyMap()

        val result = mutableMapOf<String, ZipEntryMeta>()
        var cursor = 0

        while (cursor + CENTRAL_FILE_HEADER_FIXED_SIZE <= cd.size) {
            if (cd.readUInt32(cursor) != CENTRAL_FILE_HEADER_SIGNATURE) break

            val compressionMethod = cd.readUInt16(cursor + 10)
            val compressedSize = cd.readUInt32(cursor + 20)
            val fileNameLength = cd.readUInt16(cursor + 28)
            val extraFieldLength = cd.readUInt16(cursor + 30)
            val commentLength = cd.readUInt16(cursor + 32)
            val localHeaderOffset = cd.readUInt32(cursor + 42)

            val nameStart = cursor + CENTRAL_FILE_HEADER_FIXED_SIZE
            val nameEnd = nameStart + fileNameLength
            if (nameEnd > cd.size) break

            val name = cd.decodeToString(nameStart, nameEnd)
            result[name] = ZipEntryMeta(
                compressionMethod = compressionMethod,
                compressedSize = compressedSize,
                localHeaderOffset = localHeaderOffset
            )

            cursor = nameEnd + extraFieldLength + commentLength
        }

        return result
    }

    private fun findEocdOffset(): Long? {
        // EOCD is always at the last 22 bytes for archives without a comment
        val eocdStart = fileSize - EOCD_RECORD_SIZE
        if (eocdStart < 0) return null

        // Fast path: read only 22 bytes from the end
        val tail = fileSystem.readBytesAt(archivePath, eocdStart, EOCD_RECORD_SIZE) ?: return null
        if (tail.readUInt32(0) == EOCD_SIGNATURE) return eocdStart

        // Fallback: scan the last 64 KB for archives with a comment
        val scanSize = minOf(MAX_EOCD_SEARCH.toLong(), fileSize).toInt()
        val scanStart = fileSize - scanSize
        val scanBuf = fileSystem.readBytesAt(archivePath, scanStart, scanSize) ?: return null

        var cursor = scanBuf.size - EOCD_RECORD_SIZE
        while (cursor >= 0) {
            if (scanBuf.readUInt32(cursor) == EOCD_SIGNATURE) return scanStart + cursor
            cursor--
        }
        return null
    }

    private data class ZipEntryMeta(
        val compressionMethod: Int,
        val compressedSize: Int,
        val localHeaderOffset: Int
    )

    private companion object {
        const val COMPRESSION_STORED = 0

        const val EOCD_SIGNATURE = 0x06054b50
        const val EOCD_RECORD_SIZE = 22
        const val MAX_EOCD_SEARCH = 65535 + EOCD_RECORD_SIZE

        const val CENTRAL_FILE_HEADER_SIGNATURE = 0x02014b50
        const val CENTRAL_FILE_HEADER_FIXED_SIZE = 46

        const val LOCAL_FILE_HEADER_SIGNATURE = 0x04034b50
        const val LOCAL_FILE_HEADER_SIZE = 30
    }
}

private fun ByteArray.readUInt16(offset: Int): Int =
    (this[offset].toInt() and 0xFF) or ((this[offset + 1].toInt() and 0xFF) shl 8)

private fun ByteArray.readUInt32(offset: Int): Int =
    (this[offset].toInt() and 0xFF) or
        ((this[offset + 1].toInt() and 0xFF) shl 8) or
        ((this[offset + 2].toInt() and 0xFF) shl 16) or
        ((this[offset + 3].toInt() and 0xFF) shl 24)
