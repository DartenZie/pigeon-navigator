package cz.miroslavpasek.pigeonnavigator.data

/**
 * Represents one location update from the platform location service.
 *
 * @property latitude Latitude in decimal degrees.
 * @property longitude Longitude in decimal degrees.
 * @property altitudeMeters Altitude above mean sea level in meters.
 * @property speedMetersPerSecond Groundspeed in meters per second.
 * @property bearingDegrees Course angle in degrees.
 * @property horizontalAccuracyMeters Estimated horizontal accuracy radius in meters.
 * @property requiresPermission True when location data cannot be provided until permission is granted.
 */
data class FlightLocation(
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double,
    val speedMetersPerSecond: Float,
    val bearingDegrees: Float,
    val horizontalAccuracyMeters: Double? = null,
    val requiresPermission: Boolean = false
)
