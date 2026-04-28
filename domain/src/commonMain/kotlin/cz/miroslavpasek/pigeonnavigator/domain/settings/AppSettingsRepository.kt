package cz.miroslavpasek.pigeonnavigator.domain.settings

import cz.miroslavpasek.pigeonnavigator.core.util.result.AppResult
import cz.miroslavpasek.pigeonnavigator.domain.failure.Failure
import kotlinx.coroutines.flow.StateFlow

/**
 * Domain contract for reading and updating global application settings.
 *
 * Implementations must:
 * - Always emit a complete [AppSettings] value through [settings].
 * - Substitute defaults for missing or invalid persisted values.
 * - Persist updates and propagate them through [settings] before returning success.
 * - Map persistence errors to explicit [Failure] values rather than throwing.
 */
interface AppSettingsRepository {
    /**
     * Always-on stream of the latest known settings. Starts with [AppSettings] defaults until
     * persisted values have been loaded.
     */
    val settings: StateFlow<AppSettings>

    /**
     * Persists [units] and updates [settings] with the new value.
     */
    suspend fun updateUnits(units: UnitPreferences): AppResult<Unit, Failure>

    /**
     * Persists [warning] and updates [settings] with the new value.
     *
     * Returns [Failure.Validation] when [WarningPreferences.timeToCollisionWarningSeconds] is
     * outside the supported range and avoids persisting the invalid value.
     */
    suspend fun updateWarningPreferences(
        warning: WarningPreferences
    ): AppResult<Unit, Failure>

    /**
     * Persists [search] and updates [settings] with the new value.
     *
     * Returns [Failure.Validation] when any field falls outside its supported range and avoids
     * persisting the invalid value.
     */
    suspend fun updateSearchPreferences(
        search: SearchPreferences
    ): AppResult<Unit, Failure>

    /**
     * Persists [map] and updates [settings] with the new value.
     *
     * Returns [Failure.Validation] when any field falls outside its supported range and avoids
     * persisting the invalid value.
     */
    suspend fun updateMapPreferences(
        map: MapPreferences
    ): AppResult<Unit, Failure>
}
