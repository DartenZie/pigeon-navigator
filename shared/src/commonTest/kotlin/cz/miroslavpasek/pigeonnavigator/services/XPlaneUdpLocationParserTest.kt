package cz.miroslavpasek.pigeonnavigator.services

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class XPlaneUdpLocationParserTest {

    @Test
    fun `parse returns location from index 20 DATA packet`() {
        val packet = xplaneDataPacket(
            latitude = 50.0755f,
            longitude = 14.4378f,
            altitudeFeet = 1_352.0f,
        )

        val location = XPlaneUdpLocationParser.parse(packet)

        assertNotNull(location)
        assertEquals(50.0755, location.latitude, absoluteTolerance = 0.00001)
        assertEquals(14.4378, location.longitude, absoluteTolerance = 0.00001)
        assertEquals(412.0896, location.altitudeMeters, absoluteTolerance = 0.0001)
        assertEquals(0f, location.speedMetersPerSecond)
        assertEquals(0f, location.bearingDegrees)
        assertEquals(false, location.requiresPermission)
    }

    @Test
    fun `parse skips other records and returns index 20`() {
        val packet = byteArrayOf('D'.code.toByte(), 'A'.code.toByte(), 'T'.code.toByte(), 'A'.code.toByte(), 0) +
            xplaneRecord(index = 3, values = FloatArray(8) { 0f }) +
            xplaneRecord(index = 20, values = floatArrayOf(49.2f, 16.6f, 900f, 0f, 0f, 0f, 0f, 0f))

        val location = XPlaneUdpLocationParser.parse(packet)

        assertNotNull(location)
        assertEquals(49.2, location.latitude, absoluteTolerance = 0.00001)
        assertEquals(16.6, location.longitude, absoluteTolerance = 0.00001)
    }

    @Test
    fun `parse returns null for non DATA prologue`() {
        val packet = xplaneDataPacket(latitude = 50f, longitude = 14f, altitudeFeet = 1000f)
        packet[0] = 'X'.code.toByte()

        assertNull(XPlaneUdpLocationParser.parse(packet))
    }

    @Test
    fun `parse returns null for malformed packet`() {
        assertNull(XPlaneUdpLocationParser.parse(ByteArray(40)))
    }

    @Test
    fun `parse returns null for invalid coordinates`() {
        val packet = xplaneDataPacket(latitude = 100f, longitude = 14f, altitudeFeet = 1000f)

        assertNull(XPlaneUdpLocationParser.parse(packet))
    }
}

private fun xplaneDataPacket(latitude: Float, longitude: Float, altitudeFeet: Float): ByteArray =
    byteArrayOf('D'.code.toByte(), 'A'.code.toByte(), 'T'.code.toByte(), 'A'.code.toByte(), 0) +
        xplaneRecord(index = 20, values = floatArrayOf(latitude, longitude, altitudeFeet, 0f, 0f, 0f, 0f, 0f))

private fun xplaneRecord(index: Int, values: FloatArray): ByteArray {
    assertTrue(values.size == 8)
    val bytes = ByteArray(36)
    bytes.writeLittleEndianInt(offset = 0, value = index)
    values.forEachIndexed { valueIndex, value ->
        bytes.writeLittleEndianInt(offset = 4 + valueIndex * 4, value = value.toBits())
    }
    return bytes
}

private fun ByteArray.writeLittleEndianInt(offset: Int, value: Int) {
    this[offset] = (value and 0xFF).toByte()
    this[offset + 1] = ((value ushr 8) and 0xFF).toByte()
    this[offset + 2] = ((value ushr 16) and 0xFF).toByte()
    this[offset + 3] = ((value ushr 24) and 0xFF).toByte()
}
