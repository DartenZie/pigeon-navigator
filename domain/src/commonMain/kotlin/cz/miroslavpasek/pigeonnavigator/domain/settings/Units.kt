package cz.miroslavpasek.pigeonnavigator.domain.settings

/**
 * Distance unit preferences used at presentation/formatting boundaries.
 *
 * Domain calculations remain in canonical meters; this enum drives display formatting only.
 */
enum class DistanceUnit {
    NauticalMiles,
    Kilometers,
    Miles,
}

/**
 * Altitude unit preferences used at presentation/formatting boundaries.
 *
 * Domain calculations remain in canonical meters; this enum drives display formatting only.
 */
enum class AltitudeUnit {
    Feet,
    Meters,
}

/**
 * Speed unit preferences used at presentation/formatting boundaries.
 *
 * Domain calculations remain in canonical meters per second; this enum drives display formatting only.
 */
enum class SpeedUnit {
    Knots,
    KilometersPerHour,
    MilesPerHour,
    MetersPerSecond,
}
