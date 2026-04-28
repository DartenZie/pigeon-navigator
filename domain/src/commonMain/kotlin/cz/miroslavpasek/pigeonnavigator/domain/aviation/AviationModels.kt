package cz.miroslavpasek.pigeonnavigator.domain.aviation

/**
 * Airport reference data loaded from an installed OFPKG package.
 */
data class Airport(
    val id: String,
    val name: String,
    val kind: String,
    val latitude: Double,
    val longitude: Double,
    val elevationMeters: Int?
)

/**
 * Navaid reference data loaded from an installed OFPKG package.
 */
data class Navaid(
    val id: String,
    val name: String,
    val kind: String,
    val detail: String,
    val frequency: String?,
    val latitude: Double,
    val longitude: Double
)

/**
 * Airspace with altitude limits and polygon geometry.
 */
data class Airspace(
    val id: String,
    val name: String,
    val kind: String,
    val lowerLimitMeters: Int?,
    val lowerLimitReference: String?,
    val upperLimitMeters: Int?,
    val upperLimitReference: String?,
    val points: List<GeoPoint>
)

/**
 * Single WGS84 coordinate.
 */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double
)

/**
 * Airport plus computed distance from query coordinate.
 */
data class NearbyAirport(
    val airport: Airport,
    val distanceMeters: Double
)

/**
 * Navaid plus computed distance from query coordinate.
 */
data class NearbyNavaid(
    val navaid: Navaid,
    val distanceMeters: Double
)
