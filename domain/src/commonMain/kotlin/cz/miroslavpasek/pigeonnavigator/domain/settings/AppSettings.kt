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
 * Aggregated user-controllable application settings, grouped by concern.
 *
 * Each nested value object owns one slice of preferences so future settings can be added
 * as new groups without breaking existing consumers.
 */
data class AppSettings(
    val units: UnitPreferences = UnitPreferences(),
    val warning: WarningPreferences = WarningPreferences(),
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
