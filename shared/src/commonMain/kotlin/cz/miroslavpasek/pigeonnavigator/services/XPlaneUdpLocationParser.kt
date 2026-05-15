package cz.miroslavpasek.pigeonnavigator.services

import cz.miroslavpasek.pigeonnavigator.data.FlightLocation
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.time.TimeSource

private const val XPLANE_PROLOGUE_SIZE = 5
private const val XPLANE_RECORD_SIZE = 36
private const val XPLANE_LOCATION_INDEX = 20
private const val FEET_TO_METERS = 0.3048
private const val EARTH_RADIUS_METERS = 6_371_000.0
private const val NANOS_PER_SECOND = 1_000_000_000.0

class XPlaneUdpLocationParser(
    private val nowNanos: () -> Long = monotonicNowNanos()
) : UdpLocationParser {
    private var previousSnapshot: Snapshot? = null

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

                val currentSnapshot = Snapshot(
                    latitude = latitude,
                    longitude = longitude,
                    timeNanos = nowNanos(),
                )
                val previous = previousSnapshot
                previousSnapshot = currentSnapshot

                val speedAndBearing = previous?.deriveSpeedAndBearing(to = currentSnapshot)
                    ?: SpeedAndBearing(speedMetersPerSecond = 0f, bearingDegrees = 0f)

                return FlightLocation(
                    latitude = latitude,
                    longitude = longitude,
                    altitudeMeters = altitudeFeet * FEET_TO_METERS,
                    speedMetersPerSecond = speedAndBearing.speedMetersPerSecond,
                    bearingDegrees = speedAndBearing.bearingDegrees,
                    horizontalAccuracyMeters = null,
                    requiresPermission = false,
                )
            }
            recordOffset += XPLANE_RECORD_SIZE
        }

        return null
    }

    private data class Snapshot(
        val latitude: Double,
        val longitude: Double,
        val timeNanos: Long,
    )

    private data class SpeedAndBearing(
        val speedMetersPerSecond: Float,
        val bearingDegrees: Float,
    )

    private fun Snapshot.deriveSpeedAndBearing(to: Snapshot): SpeedAndBearing {
        val elapsedSeconds = (to.timeNanos - timeNanos) / NANOS_PER_SECOND
        if (elapsedSeconds <= 0.0) {
            return SpeedAndBearing(speedMetersPerSecond = 0f, bearingDegrees = 0f)
        }

        val distanceMeters = distanceMetersTo(to)
        return SpeedAndBearing(
            speedMetersPerSecond = (distanceMeters / elapsedSeconds).toFloat(),
            bearingDegrees = bearingDegreesTo(to).toFloat(),
        )
    }

    private fun Snapshot.distanceMetersTo(to: Snapshot): Double {
        val lat1 = latitude.toRadians()
        val lat2 = to.latitude.toRadians()
        val deltaLat = (to.latitude - latitude).toRadians()
        val deltaLon = (to.longitude - longitude).toRadians()

        val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
            cos(lat1) * cos(lat2) * sin(deltaLon / 2) * sin(deltaLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    private fun Snapshot.bearingDegreesTo(to: Snapshot): Double {
        val lat1 = latitude.toRadians()
        val lat2 = to.latitude.toRadians()
        val deltaLon = (to.longitude - longitude).toRadians()

        val y = sin(deltaLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(deltaLon)
        return (atan2(y, x).toDegrees() + 360.0) % 360.0
    }

    private fun Double.toRadians(): Double = this * PI / 180.0

    private fun Double.toDegrees(): Double = this * 180.0 / PI

    companion object {
        private val monotonicOrigin = TimeSource.Monotonic.markNow()

        private fun monotonicNowNanos(): () -> Long = {
            monotonicOrigin.elapsedNow().inWholeNanoseconds
        }
    }
}

private fun ByteArray.readLittleEndianInt(offset: Int): Int =
    (this[offset].toInt() and 0xFF) or
        ((this[offset + 1].toInt() and 0xFF) shl 8) or
        ((this[offset + 2].toInt() and 0xFF) shl 16) or
        ((this[offset + 3].toInt() and 0xFF) shl 24)

private fun ByteArray.readLittleEndianFloat(offset: Int): Float =
    Float.fromBits(readLittleEndianInt(offset))
