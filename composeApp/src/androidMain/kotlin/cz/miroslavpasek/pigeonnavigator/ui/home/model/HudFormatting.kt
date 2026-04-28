package cz.miroslavpasek.pigeonnavigator.ui.home.model

import cz.miroslavpasek.pigeonnavigator.domain.settings.AltitudeUnit
import cz.miroslavpasek.pigeonnavigator.domain.settings.SpeedUnit
import kotlin.math.roundToInt

/**
 * Display-side conversions for altitude and speed used by the HUD cluster.
 *
 * Lives in the home UI layer so the domain enums stay free of formatting
 * concerns.
 */
internal fun Double.toDisplayAltitude(unit: AltitudeUnit): Int = when (unit) {
    AltitudeUnit.Feet -> (this * 3.280839895).roundToInt()
    AltitudeUnit.Meters -> roundToInt()
}

internal fun Float.toDisplaySpeed(unit: SpeedUnit): Int = toDouble().toDisplaySpeed(unit)

internal fun Double.toDisplaySpeed(unit: SpeedUnit): Int = when (unit) {
    SpeedUnit.Knots -> (this * 1.943844492).roundToInt()
    SpeedUnit.KilometersPerHour -> (this * 3.6).roundToInt()
    SpeedUnit.MilesPerHour -> (this * 2.236936292).roundToInt()
    SpeedUnit.MetersPerSecond -> roundToInt()
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
