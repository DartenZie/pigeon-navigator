package cz.miroslavpasek.pigeonnavigator.data.settings.internal

import cz.miroslavpasek.pigeonnavigator.domain.settings.AltitudeUnit
import cz.miroslavpasek.pigeonnavigator.domain.settings.DistanceUnit
import cz.miroslavpasek.pigeonnavigator.domain.settings.SpeedUnit

/**
 * Maps [DistanceUnit] to a stable serialized name. Persisting the enum name (not the ordinal)
 * keeps the serialized form resilient to enum reordering.
 */
internal fun DistanceUnit.serializedName(): String = when (this) {
    DistanceUnit.NauticalMiles -> "nautical_miles"
    DistanceUnit.Kilometers -> "kilometers"
    DistanceUnit.Miles -> "miles"
}

/**
 * Parses a previously serialized [DistanceUnit] name. Returns `null` for unknown or absent values
 * so the caller can fall back to the documented default.
 */
internal fun distanceUnitFromSerialized(value: String?): DistanceUnit? = when (value) {
    "nautical_miles" -> DistanceUnit.NauticalMiles
    "kilometers" -> DistanceUnit.Kilometers
    "miles" -> DistanceUnit.Miles
    else -> null
}

/**
 * Maps [AltitudeUnit] to a stable serialized name.
 */
internal fun AltitudeUnit.serializedName(): String = when (this) {
    AltitudeUnit.Feet -> "feet"
    AltitudeUnit.Meters -> "meters"
}

/**
 * Parses a previously serialized [AltitudeUnit] name.
 */
internal fun altitudeUnitFromSerialized(value: String?): AltitudeUnit? = when (value) {
    "feet" -> AltitudeUnit.Feet
    "meters" -> AltitudeUnit.Meters
    else -> null
}

/**
 * Maps [SpeedUnit] to a stable serialized name.
 */
internal fun SpeedUnit.serializedName(): String = when (this) {
    SpeedUnit.Knots -> "knots"
    SpeedUnit.KilometersPerHour -> "kilometers_per_hour"
    SpeedUnit.MilesPerHour -> "miles_per_hour"
    SpeedUnit.MetersPerSecond -> "meters_per_second"
}

/**
 * Parses a previously serialized [SpeedUnit] name.
 */
internal fun speedUnitFromSerialized(value: String?): SpeedUnit? = when (value) {
    "knots" -> SpeedUnit.Knots
    "kilometers_per_hour" -> SpeedUnit.KilometersPerHour
    "miles_per_hour" -> SpeedUnit.MilesPerHour
    "meters_per_second" -> SpeedUnit.MetersPerSecond
    else -> null
}
