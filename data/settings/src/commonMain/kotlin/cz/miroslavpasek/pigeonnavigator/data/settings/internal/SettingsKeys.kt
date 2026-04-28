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
    const val SEARCH_DEBOUNCE_MILLIS = "app_settings.search.debounce_millis"
    const val MINIMUM_SEARCH_QUERY_LENGTH = "app_settings.search.minimum_query_length"
    const val MAX_DYNAMIC_ZOOM_SPEED_KMH = "app_settings.map.max_dynamic_zoom_speed_kmh"
    const val MAX_SPEED_ZOOM_OUT_DELTA = "app_settings.map.max_speed_zoom_out_delta"
    const val BEARING_UPDATE_THRESHOLD_DEGREES = "app_settings.map.bearing_update_threshold_degrees"
}
