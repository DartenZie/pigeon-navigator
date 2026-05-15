package cz.miroslavpasek.pigeonnavigator.services

import cz.miroslavpasek.pigeonnavigator.data.FlightLocation

private const val XPLANE_PROLOGUE_SIZE = 5
private const val XPLANE_RECORD_SIZE = 36
private const val XPLANE_LOCATION_INDEX = 20
private const val FEET_TO_METERS = 0.3048

object XPlaneUdpLocationParser : UdpLocationParser {
    override fun parse(packet: ByteArray, offset: Int, length: Int): FlightLocation? {
        if (offset < 0 || length < XPLANE_PROLOGUE_SIZE + XPLANE_RECORD_SIZE || offset + length > packet.size) {
            return null
        }
        if (
            packet[offset] != 'D'.code.toByte() ||
            packet[offset + 1] != 'A'.code.toByte() ||
            packet[offset + 2] != 'T'.code.toByte() ||
            packet[offset + 3] != 'A'.code.toByte()
        ) {
            return null
        }

        var recordOffset = offset + XPLANE_PROLOGUE_SIZE
        val endOffset = offset + length
        while (recordOffset + XPLANE_RECORD_SIZE <= endOffset) {
            val index = packet.readLittleEndianInt(recordOffset)
            if (index == XPLANE_LOCATION_INDEX) {
                val latitude = packet.readLittleEndianFloat(recordOffset + 4).toDouble()
                val longitude = packet.readLittleEndianFloat(recordOffset + 8).toDouble()
                val altitudeFeet = packet.readLittleEndianFloat(recordOffset + 12).toDouble()

                if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) {
                    return null
                }

                return FlightLocation(
                    latitude = latitude,
                    longitude = longitude,
                    altitudeMeters = altitudeFeet * FEET_TO_METERS,
                    speedMetersPerSecond = 0f,
                    bearingDegrees = 0f,
                    horizontalAccuracyMeters = null,
                    requiresPermission = false,
                )
            }
            recordOffset += XPLANE_RECORD_SIZE
        }

        return null
    }
}

private fun ByteArray.readLittleEndianInt(offset: Int): Int =
    (this[offset].toInt() and 0xFF) or
        ((this[offset + 1].toInt() and 0xFF) shl 8) or
        ((this[offset + 2].toInt() and 0xFF) shl 16) or
        ((this[offset + 3].toInt() and 0xFF) shl 24)

private fun ByteArray.readLittleEndianFloat(offset: Int): Float =
    Float.fromBits(readLittleEndianInt(offset))
