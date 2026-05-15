package cz.miroslavpasek.pigeonnavigator.services

import cz.miroslavpasek.pigeonnavigator.data.FlightLocation

private const val MSFS_PACKET_PREFIX = "XGPSMSFS"
private const val MSFS_PACKET_FIELDS = 6
private const val MSFS_PACKET_FIELDS_WITH_COMMA_DECIMAL_LONGITUDE = 7

interface UdpLocationParser {
    fun parse(packet: ByteArray, offset: Int = 0, length: Int = packet.size): FlightLocation?
}

object MsfsUdpLocationParser : UdpLocationParser {
    override fun parse(packet: ByteArray, offset: Int, length: Int): FlightLocation? {
        if (offset < 0 || length < 0 || offset + length > packet.size) return null
        return parseMsfsUdpLocationPacket(packet.decodeToString(startIndex = offset, endIndex = offset + length))
    }
}

/**
 * Parses UDP packets produced by msfs-2020-gps-link.
 *
 * Packet format:
 * XGPSMSFS,<lon>,<lat>,<alt_m>,<track_deg>,<speed_mps>
 */
fun parseMsfsUdpLocationPacket(payload: String): FlightLocation? {
    val fields = payload.trim().split(',')
    if (fields.size < MSFS_PACKET_FIELDS || fields[0] != MSFS_PACKET_PREFIX) {
        return null
    }

    val values = if (fields.size == MSFS_PACKET_FIELDS_WITH_COMMA_DECIMAL_LONGITUDE) {
        listOf(
            fields[1] + "." + fields[2],
            fields[3],
            fields[4],
            fields[5],
            fields[6],
        )
    } else {
        fields.drop(1)
    }

    val longitude = values[0].toDoubleOrNull() ?: return null
    val latitude = values[1].toDoubleOrNull() ?: return null
    val altitudeMeters = values[2].toDoubleOrNull() ?: return null
    val bearingDegrees = values[3].toFloatOrNull() ?: return null
    val speedMetersPerSecond = values[4].toFloatOrNull() ?: return null

    if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) {
        return null
    }

    return FlightLocation(
        latitude = latitude,
        longitude = longitude,
        altitudeMeters = altitudeMeters,
        speedMetersPerSecond = speedMetersPerSecond,
        bearingDegrees = bearingDegrees,
        horizontalAccuracyMeters = null,
        requiresPermission = false,
    )
}
