package cz.miroslavpasek.pigeonnavigator.data.settings.internal

/**
 * Stable persistence keys for the settings data layer.
 *
 * Keys are kept stable to allow forward-compatible migrations. Adding a new setting must
 * introduce a new key; existing keys must never be repurposed.
 */
internal object SettingsKeys {
    const val DISTANCE_UNIT = "app_settings.units.distance"
    const val ALTITUDE_UNIT = "app_settings.units.altitude"
    const val SPEED_UNIT = "app_settings.units.speed"
    const val TIME_TO_COLLISION_WARNING_SECONDS = "app_settings.warning.time_to_collision_seconds"
}
