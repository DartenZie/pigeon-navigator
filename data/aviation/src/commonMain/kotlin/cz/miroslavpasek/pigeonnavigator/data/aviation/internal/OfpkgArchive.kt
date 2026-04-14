package cz.miroslavpasek.pigeonnavigator.data.aviation.internal

import cz.miroslavpasek.pigeonnavigator.data.aviation.platform.DeflateDecoder

/**
 * Minimal ZIP reader for OFPKG archives.
 */
internal class OfpkgArchive(
    private val archiveBytes: ByteArray,
    private val deflateDecoder: DeflateDecoder = DeflateDecoder()
) {
    private val entriesByName: Map<String, ZipEntryMeta> = parseCentralDirectory().associateBy { it.name }

    fun readEntry(name: String): ByteArray? {
        val meta = entriesByName[name] ?: return null
        val localHeaderOffset = meta.localHeaderOffset
        if (localHeaderOffset + LOCAL_FILE_HEADER_SIZE > archiveBytes.size) {
            return null
        }

        if (archiveBytes.readUInt32(localHeaderOffset) != LOCAL_FILE_HEADER_SIGNATURE) {
            return null
        }

        val fileNameLength = archiveBytes.readUInt16(localHeaderOffset + 26)
        val extraFieldLength = archiveBytes.readUInt16(localHeaderOffset + 28)
        val dataStart = localHeaderOffset + LOCAL_FILE_HEADER_SIZE + fileNameLength + extraFieldLength
        val dataEnd = dataStart + meta.compressedSize

        if (dataStart < 0 || dataEnd > archiveBytes.size || dataStart >= dataEnd) {
            return null
        }

        val compressed = archiveBytes.copyOfRange(dataStart, dataEnd)
        return when (meta.compressionMethod) {
            COMPRESSION_STORED -> compressed
            COMPRESSION_DEFLATE -> deflateDecoder.decode(compressed)
            else -> null
        }
    }

    private fun parseCentralDirectory(): List<ZipEntryMeta> {
        val eocdOffset = findEndOfCentralDirectoryOffset() ?: return emptyList()
        val centralDirectorySize = archiveBytes.readUInt32(eocdOffset + 12)
        val centralDirectoryOffset = archiveBytes.readUInt32(eocdOffset + 16)

        if (centralDirectoryOffset < 0 || centralDirectorySize <= 0) {
            return emptyList()
        }

        val end = centralDirectoryOffset + centralDirectorySize
        if (end > archiveBytes.size) {
            return emptyList()
        }

        val result = mutableListOf<ZipEntryMeta>()
        var cursor = centralDirectoryOffset

        while (cursor + CENTRAL_FILE_HEADER_FIXED_SIZE <= end) {
            if (archiveBytes.readUInt32(cursor) != CENTRAL_FILE_HEADER_SIGNATURE) {
                break
            }

            val compressionMethod = archiveBytes.readUInt16(cursor + 10)
            val compressedSize = archiveBytes.readUInt32(cursor + 20)
            val fileNameLength = archiveBytes.readUInt16(cursor + 28)
            val extraFieldLength = archiveBytes.readUInt16(cursor + 30)
            val commentLength = archiveBytes.readUInt16(cursor + 32)
            val localHeaderOffset = archiveBytes.readUInt32(cursor + 42)
            val nameStart = cursor + CENTRAL_FILE_HEADER_FIXED_SIZE
            val nameEnd = nameStart + fileNameLength
            if (nameEnd > archiveBytes.size) {
                break
            }

            val name = archiveBytes.decodeToString(nameStart, nameEnd)
            result += ZipEntryMeta(
                name = name,
                compressionMethod = compressionMethod,
                compressedSize = compressedSize,
                localHeaderOffset = localHeaderOffset
            )

            cursor = nameEnd + extraFieldLength + commentLength
        }

        return result
    }

    private fun findEndOfCentralDirectoryOffset(): Int? {
        val minimumOffset = (archiveBytes.size - MAX_EOCD_SEARCH).coerceAtLeast(0)
        var cursor = archiveBytes.size - EOCD_RECORD_SIZE
        while (cursor >= minimumOffset) {
            if (archiveBytes.readUInt32(cursor) == EOCD_SIGNATURE) {
                return cursor
            }
            cursor -= 1
        }
        return null
    }

    private data class ZipEntryMeta(
        val name: String,
        val compressionMethod: Int,
        val compressedSize: Int,
        val localHeaderOffset: Int
    )

    private companion object {
        const val COMPRESSION_STORED = 0
        const val COMPRESSION_DEFLATE = 8

        const val EOCD_SIGNATURE = 0x06054b50
        const val EOCD_RECORD_SIZE = 22
        const val MAX_EOCD_SEARCH = 65535 + EOCD_RECORD_SIZE

        const val CENTRAL_FILE_HEADER_SIGNATURE = 0x02014b50
        const val CENTRAL_FILE_HEADER_FIXED_SIZE = 46

        const val LOCAL_FILE_HEADER_SIGNATURE = 0x04034b50
        const val LOCAL_FILE_HEADER_SIZE = 30
    }
}

private fun ByteArray.readUInt16(offset: Int): Int {
    return (this[offset].toInt() and 0xFF) or
        ((this[offset + 1].toInt() and 0xFF) shl 8)
}

private fun ByteArray.readUInt32(offset: Int): Int {
    return (this[offset].toInt() and 0xFF) or
        ((this[offset + 1].toInt() and 0xFF) shl 8) or
        ((this[offset + 2].toInt() and 0xFF) shl 16) or
        ((this[offset + 3].toInt() and 0xFF) shl 24)
}
