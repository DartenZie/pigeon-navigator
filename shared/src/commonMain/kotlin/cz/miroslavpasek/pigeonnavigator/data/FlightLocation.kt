package cz.miroslavpasek.pigeonnavigator.data

data class FlightLocation(
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double,
    val speedMetersPerSecond: Float,
    val bearingDegrees: Float,
    val requiresPermission: Boolean = false
)
