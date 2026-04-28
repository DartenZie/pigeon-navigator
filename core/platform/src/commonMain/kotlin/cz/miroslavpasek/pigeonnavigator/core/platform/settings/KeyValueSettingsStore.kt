package cz.miroslavpasek.pigeonnavigator.core.platform.settings

/**
 * Primitive platform-agnostic key/value storage for small persisted settings values.
 *
 * Implementations are platform-specific (e.g. Android `SharedPreferences`, iOS `NSUserDefaults`)
 * and bound through DI in platform composition roots. Shared consumers must depend only on
 * this interface and never on platform SDK types.
 *
 * Reads must return `null` when a key is absent so callers can fall back to defaults instead of
 * misinterpreting type-default values such as `0`.
 */
interface KeyValueSettingsStore {
    /**
     * Returns the string value persisted under [key] or `null` when absent.
     */
    suspend fun getString(key: String): String?

    /**
     * Persists [value] under [key].
     */
    suspend fun putString(key: String, value: String)

    /**
     * Returns the integer value persisted under [key] or `null` when absent.
     */
    suspend fun getInt(key: String): Int?

    /**
     * Persists [value] under [key].
     */
    suspend fun putInt(key: String, value: Int)
}
