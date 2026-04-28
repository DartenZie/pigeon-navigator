package cz.miroslavpasek.pigeonnavigator.services

import cz.miroslavpasek.pigeonnavigator.data.FlightLocation

private const val MSFS_PACKET_PREFIX = "XGPSMSFS"
private const val MSFS_PACKET_FIELDS = 6

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

    val longitude = fields[1].toDoubleOrNull() ?: return null
    val latitude = fields[2].toDoubleOrNull() ?: return null
    val altitudeMeters = fields[3].toDoubleOrNull() ?: return null
    val bearingDegrees = fields[4].toFloatOrNull() ?: return null
    val speedMetersPerSecond = fields[5].toFloatOrNull() ?: return null

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
