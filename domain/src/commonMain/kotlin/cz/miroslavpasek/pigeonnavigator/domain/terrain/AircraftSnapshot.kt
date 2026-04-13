package cz.miroslavpasek.pigeonnavigator.domain.terrain

/**
 * Captures aircraft telemetry used for terrain conflict prediction.
 *
 * @property latitude Current latitude in decimal degrees.
 * @property longitude Current longitude in decimal degrees.
 * @property altitudeMeters Current altitude above mean sea level in meters.
 * @property speedMetersPerSecond Current groundspeed in meters per second.
 * @property bearingDegrees Current course angle in degrees in the [0, 360) range.
 */
data class AircraftSnapshot(
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double,
    val speedMetersPerSecond: Double,
    val bearingDegrees: Double
)
