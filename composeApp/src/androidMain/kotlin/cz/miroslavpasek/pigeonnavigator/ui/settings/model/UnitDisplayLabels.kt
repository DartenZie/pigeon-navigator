package cz.miroslavpasek.pigeonnavigator.ui.settings.model

import cz.miroslavpasek.pigeonnavigator.domain.settings.AltitudeUnit
import cz.miroslavpasek.pigeonnavigator.domain.settings.DistanceUnit
import cz.miroslavpasek.pigeonnavigator.domain.settings.SpeedUnit

/**
 * Presentation labels for unit-preference enums.
 *
 * Lives in the settings UI layer so domain enums stay free of display strings.
 */
internal fun DistanceUnit.displayLabel(): String = when (this) {
    DistanceUnit.NauticalMiles -> "NM"
    DistanceUnit.Kilometers -> "km"
    DistanceUnit.Miles -> "mi"
}

internal fun AltitudeUnit.displayLabel(): String = when (this) {
    AltitudeUnit.Feet -> "ft"
    AltitudeUnit.Meters -> "m"
}

internal fun SpeedUnit.displayLabel(): String = when (this) {
    SpeedUnit.Knots -> "kt"
    SpeedUnit.KilometersPerHour -> "km/h"
    SpeedUnit.MilesPerHour -> "mph"
    SpeedUnit.MetersPerSecond -> "m/s"
}
