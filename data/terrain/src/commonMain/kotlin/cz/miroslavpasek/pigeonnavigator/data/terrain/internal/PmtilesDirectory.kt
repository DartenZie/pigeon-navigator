package cz.miroslavpasek.pigeonnavigator.data.terrain.internal

/**
 * Describes one PMTiles directory entry and its tile id run.
 */
internal data class PmtilesDirectoryEntry(
    val tileId: Long,
    val runLength: Int,
    val length: Int,
    val offset: Int
)

/**
 * Performs lookup and decoding of PMTiles directory entries.
 */
internal class PmtilesDirectory(
    private val entries: List<PmtilesDirectoryEntry>
) {
    /**
     * Returns the entry whose run includes [tileId], or `null` when not present.
     */
    fun findEntry(tileId: Long): PmtilesDirectoryEntry? {
        if (entries.isEmpty()) {
            return null
        }

        var low = 0
        var high = entries.size - 1

        while (low <= high) {
            val mid = (low + high) ushr 1
            val entry = entries[mid]

            if (tileId < entry.tileId) {
                high = mid - 1
                continue
            }

            if (entry.runLength <= 0) {
                low = mid + 1
                continue
            }

            val endExclusive = entry.tileId + entry.runLength
            if (tileId >= endExclusive) {
                low = mid + 1
                continue
            }

            return entry
        }

        return null
    }

    companion object {
        /**
         * Decodes a PMTiles directory byte block into sorted entries.
         */
        fun decode(bytes: ByteArray): PmtilesDirectory {
            val cursor = VarintCursor(bytes)
            val entryCount = cursor.readVarint().toInt()

            val tileIds = LongArray(entryCount)
            var previousTileId = 0L
            for (index in 0 until entryCount) {
                previousTileId += cursor.readVarint()
                tileIds[index] = previousTileId
            }

            val runLengths = IntArray(entryCount) { cursor.readVarint().toInt() }
            val lengths = IntArray(entryCount) { cursor.readVarint().toInt() }

            val offsets = IntArray(entryCount)
            for (index in 0 until entryCount) {
                val encoded = cursor.readVarint().toInt()
                offsets[index] = if (index > 0 && encoded == 0) {
                    offsets[index - 1] + lengths[index - 1]
                } else {
                    encoded - 1
                }
            }

            val entries = List(entryCount) { index ->
                PmtilesDirectoryEntry(
                    tileId = tileIds[index],
                    runLength = runLengths[index],
                    length = lengths[index],
                    offset = offsets[index]
                )
            }

            return PmtilesDirectory(entries)
        }
    }
}

/**
 * Reads unsigned varints from a byte sequence.
 */
private class VarintCursor(
    private val bytes: ByteArray
) {
    private var position: Int = 0

    /**
     * Reads the next unsigned varint.
     *
     * @throws IllegalStateException when the byte stream ends before the varint terminates.
     */
    fun readVarint(): Long {
        var result = 0L
        var shift = 0

        while (position < bytes.size) {
            val value = bytes[position].toInt() and 0xFF
            position += 1

            result = result or ((value and 0x7F).toLong() shl shift)
            if ((value and 0x80) == 0) {
                return result
            }

            shift += 7
        }

        error("Unexpected end of varint sequence")
    }
}
