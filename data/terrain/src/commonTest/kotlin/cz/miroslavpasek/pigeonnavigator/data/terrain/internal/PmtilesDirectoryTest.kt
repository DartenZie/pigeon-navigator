package cz.miroslavpasek.pigeonnavigator.data.terrain.internal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PmtilesDirectoryTest {

    @Test
    fun decodesEntriesAndFindsMatchingRun() {
        val encoded = encodeDirectory(
            tileIds = listOf(100L, 105L, 120L),
            runLengths = listOf(3, 2, 1),
            lengths = listOf(10, 20, 30),
            offsets = listOf(0, 10, 30)
        )

        val directory = PmtilesDirectory.decode(encoded)
        val entry = directory.findEntry(106)

        assertNotNull(entry)
        assertEquals(105L, entry.tileId)
        assertEquals(20, entry.length)
        assertEquals(10, entry.offset)
    }

    @Test
    fun returnsNullWhenTileIsNotCovered() {
        val encoded = encodeDirectory(
            tileIds = listOf(20L),
            runLengths = listOf(1),
            lengths = listOf(4),
            offsets = listOf(0)
        )

        val directory = PmtilesDirectory.decode(encoded)
        assertNull(directory.findEntry(30))
    }
}

private fun encodeDirectory(
    tileIds: List<Long>,
    runLengths: List<Int>,
    lengths: List<Int>,
    offsets: List<Int>
): ByteArray {
    require(tileIds.size == runLengths.size)
    require(runLengths.size == lengths.size)
    require(lengths.size == offsets.size)

    val bytes = mutableListOf<Byte>()
    bytes.writeVarint(tileIds.size.toLong())

    var previousTile = 0L
    tileIds.forEach { tileId ->
        bytes.writeVarint(tileId - previousTile)
        previousTile = tileId
    }

    runLengths.forEach { bytes.writeVarint(it.toLong()) }
    lengths.forEach { bytes.writeVarint(it.toLong()) }

    offsets.forEachIndexed { index, offset ->
        val encoded = if (index > 0 && offset == offsets[index - 1] + lengths[index - 1]) {
            0L
        } else {
            offset.toLong() + 1L
        }
        bytes.writeVarint(encoded)
    }

    return bytes.toByteArray()
}

private fun MutableList<Byte>.writeVarint(value: Long) {
    var current = value
    while (true) {
        if ((current and 0x7FL.inv()) == 0L) {
            add(current.toByte())
            return
        }

        add(((current and 0x7F) or 0x80).toByte())
        current = current ushr 7
    }
}
