package cz.miroslavpasek.pigeonnavigator.domain.settings

/**
 * Default warning threshold (in seconds) for time-to-collision based terrain warnings.
 *
 * Centralized so all defaults stay aligned across persistence, presentation, and tests.
 */
const val DEFAULT_TIME_TO_COLLISION_WARNING_SECONDS: Int = 60

/**
 * Lower bound (inclusive) for valid time-to-collision warning thresholds, in seconds.
 */
const val MIN_TIME_TO_COLLISION_WARNING_SECONDS: Int = 5

/**
 * Upper bound (inclusive) for valid time-to-collision warning thresholds, in seconds.
 */
const val MAX_TIME_TO_COLLISION_WARNING_SECONDS: Int = 600

/**
 * Default debounce (in milliseconds) applied to the search dock before submitting a query.
 */
const val DEFAULT_SEARCH_DEBOUNCE_MILLIS: Long = 300L

/**
 * Lower bound (inclusive) for the search debounce, in milliseconds.
 */
const val MIN_SEARCH_DEBOUNCE_MILLIS: Long = 0L

/**
 * Upper bound (inclusive) for the search debounce, in milliseconds.
 */
const val MAX_SEARCH_DEBOUNCE_MILLIS: Long = 2_000L

/**
 * Default minimum query length (in characters) before automatic search submission triggers.
 */
const val DEFAULT_MINIMUM_SEARCH_QUERY_LENGTH: Int = 2

/**
 * Lower bound (inclusive) for the minimum search query length.
 */
const val MIN_MINIMUM_SEARCH_QUERY_LENGTH: Int = 1

/**
 * Upper bound (inclusive) for the minimum search query length.
 */
const val MAX_MINIMUM_SEARCH_QUERY_LENGTH: Int = 10

/**
 * Default speed (in km/h) at which the dynamic camera zoom-out reaches its maximum delta.
 */
const val DEFAULT_MAX_DYNAMIC_ZOOM_SPEED_KMH: Double = 300.0

/**
 * Lower bound (inclusive) for [DEFAULT_MAX_DYNAMIC_ZOOM_SPEED_KMH], in km/h.
 */
const val MIN_MAX_DYNAMIC_ZOOM_SPEED_KMH: Double = 50.0

/**
 * Upper bound (inclusive) for [DEFAULT_MAX_DYNAMIC_ZOOM_SPEED_KMH], in km/h.
 */
const val MAX_MAX_DYNAMIC_ZOOM_SPEED_KMH: Double = 1_500.0

/**
 * Default zoom-level delta subtracted from the base zoom when speed reaches the dynamic max.
 */
const val DEFAULT_MAX_SPEED_ZOOM_OUT_DELTA: Double = 2.5

/**
 * Lower bound (inclusive) for the speed-driven zoom-out delta, in zoom levels.
 */
const val MIN_MAX_SPEED_ZOOM_OUT_DELTA: Double = 0.0

/**
 * Upper bound (inclusive) for the speed-driven zoom-out delta, in zoom levels.
 */
const val MAX_MAX_SPEED_ZOOM_OUT_DELTA: Double = 8.0

/**
 * Default minimum bearing change (in degrees) required before the camera rotates.
 */
const val DEFAULT_BEARING_UPDATE_THRESHOLD_DEGREES: Double = 4.0

/**
 * Lower bound (inclusive) for the bearing update threshold, in degrees.
 */
const val MIN_BEARING_UPDATE_THRESHOLD_DEGREES: Double = 0.0

/**
 * Upper bound (inclusive) for the bearing update threshold, in degrees.
 */
const val MAX_BEARING_UPDATE_THRESHOLD_DEGREES: Double = 45.0

/**
 * Aggregated user-controllable application settings, grouped by concern.
 *
 * Each nested value object owns one slice of preferences so future settings can be added
 * as new groups without breaking existing consumers.
 */
data class AppSettings(
    val units: UnitPreferences = UnitPreferences(),
    val warning: WarningPreferences = WarningPreferences(),
    val search: SearchPreferences = SearchPreferences(),
    val map: MapPreferences = MapPreferences(),
    val location: LocationPreferences = LocationPreferences(),
)

/**
 * User-preferred display units for distance, altitude and speed.
 */
data class UnitPreferences(
    val distance: DistanceUnit = DistanceUnit.NauticalMiles,
    val altitude: AltitudeUnit = AltitudeUnit.Feet,
    val speed: SpeedUnit = SpeedUnit.Knots,
)

/**
 * User-controllable warning configuration.
 *
 * @property timeToCollisionWarningSeconds Time-to-impact threshold (in seconds) used by the
 * terrain warning feature to escalate to [TerrainWarningLevel.Warning].
 */
data class WarningPreferences(
    val timeToCollisionWarningSeconds: Int = DEFAULT_TIME_TO_COLLISION_WARNING_SECONDS,
)

/**
 * User-controllable search behavior used by the search dock UI.
 *
 * @property searchDebounceMillis Debounce delay before submitting a typed query.
 * @property minimumQueryLength Minimum trimmed query length required before auto-submit triggers.
 */
data class SearchPreferences(
    val searchDebounceMillis: Long = DEFAULT_SEARCH_DEBOUNCE_MILLIS,
    val minimumQueryLength: Int = DEFAULT_MINIMUM_SEARCH_QUERY_LENGTH,
)

/**
 * User-controllable map camera behavior.
 *
 * @property maxDynamicZoomSpeedKmh Speed at which the dynamic zoom-out reaches its maximum delta.
 * @property maxSpeedZoomOutDelta Maximum zoom-level delta applied at [maxDynamicZoomSpeedKmh].
 * @property bearingUpdateThresholdDegrees Minimum bearing change before the camera rotates.
 */
data class MapPreferences(
    val maxDynamicZoomSpeedKmh: Double = DEFAULT_MAX_DYNAMIC_ZOOM_SPEED_KMH,
    val maxSpeedZoomOutDelta: Double = DEFAULT_MAX_SPEED_ZOOM_OUT_DELTA,
    val bearingUpdateThresholdDegrees: Double = DEFAULT_BEARING_UPDATE_THRESHOLD_DEGREES,
)

/**
 * User-controllable source for aircraft location updates.
 */
data class LocationPreferences(
    val source: LocationSource = LocationSource.DeviceGps,
    val udpFormat: UdpLocationFormat = UdpLocationFormat.Msfs,
)

enum class LocationSource {
    DeviceGps,
    Udp,
}

enum class UdpLocationFormat {
    Msfs,
    XPlane,
}
