package cz.miroslavpasek.pigeonnavigator.data.settings

import cz.miroslavpasek.pigeonnavigator.core.platform.coroutines.DispatcherProvider
import cz.miroslavpasek.pigeonnavigator.core.platform.settings.KeyValueSettingsStore
import cz.miroslavpasek.pigeonnavigator.core.util.result.AppResult
import cz.miroslavpasek.pigeonnavigator.data.settings.internal.SettingsKeys
import cz.miroslavpasek.pigeonnavigator.data.settings.internal.altitudeUnitFromSerialized
import cz.miroslavpasek.pigeonnavigator.data.settings.internal.distanceUnitFromSerialized
import cz.miroslavpasek.pigeonnavigator.data.settings.internal.serializedName
import cz.miroslavpasek.pigeonnavigator.data.settings.internal.speedUnitFromSerialized
import cz.miroslavpasek.pigeonnavigator.domain.failure.Failure
import cz.miroslavpasek.pigeonnavigator.domain.settings.AppSettings
import cz.miroslavpasek.pigeonnavigator.domain.settings.AppSettingsRepository
import cz.miroslavpasek.pigeonnavigator.domain.settings.MAX_BEARING_UPDATE_THRESHOLD_DEGREES
import cz.miroslavpasek.pigeonnavigator.domain.settings.MAX_MAX_DYNAMIC_ZOOM_SPEED_KMH
import cz.miroslavpasek.pigeonnavigator.domain.settings.MAX_MAX_SPEED_ZOOM_OUT_DELTA
import cz.miroslavpasek.pigeonnavigator.domain.settings.MAX_MINIMUM_SEARCH_QUERY_LENGTH
import cz.miroslavpasek.pigeonnavigator.domain.settings.MAX_SEARCH_DEBOUNCE_MILLIS
import cz.miroslavpasek.pigeonnavigator.domain.settings.MAX_TIME_TO_COLLISION_WARNING_SECONDS
import cz.miroslavpasek.pigeonnavigator.domain.settings.MIN_BEARING_UPDATE_THRESHOLD_DEGREES
import cz.miroslavpasek.pigeonnavigator.domain.settings.MIN_MAX_DYNAMIC_ZOOM_SPEED_KMH
import cz.miroslavpasek.pigeonnavigator.domain.settings.MIN_MAX_SPEED_ZOOM_OUT_DELTA
import cz.miroslavpasek.pigeonnavigator.domain.settings.MIN_MINIMUM_SEARCH_QUERY_LENGTH
import cz.miroslavpasek.pigeonnavigator.domain.settings.MIN_SEARCH_DEBOUNCE_MILLIS
import cz.miroslavpasek.pigeonnavigator.domain.settings.MIN_TIME_TO_COLLISION_WARNING_SECONDS
import cz.miroslavpasek.pigeonnavigator.domain.settings.MapPreferences
import cz.miroslavpasek.pigeonnavigator.domain.settings.SearchPreferences
import cz.miroslavpasek.pigeonnavigator.domain.settings.UnitPreferences
import cz.miroslavpasek.pigeonnavigator.domain.settings.WarningPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Local-only [AppSettingsRepository] implementation backed by a [KeyValueSettingsStore].
 *
 * Cache policy: **local-only**. Settings are private to the device and never synchronized to
 * a remote backend.
 *
 * The repository immediately exposes [AppSettings] defaults via [settings] and asynchronously
 * refreshes the in-memory state from persistent storage during construction. Missing or invalid
 * persisted values resolve to defaults so reads never crash the app.
 *
 * All persistence I/O runs on [DispatcherProvider.io].
 */
class AppSettingsRepositoryImpl(
    private val store: KeyValueSettingsStore,
    private val dispatcherProvider: DispatcherProvider,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + dispatcherProvider.io)
) : AppSettingsRepository {

    private val mutableSettings = MutableStateFlow(AppSettings())
    private val writeMutex = Mutex()

    override val settings: StateFlow<AppSettings> = mutableSettings.asStateFlow()

    init {
        scope.launch { loadInitialSettings() }
    }

    override suspend fun updateUnits(units: UnitPreferences): AppResult<Unit, Failure> =
        writeMutex.withLock {
            try {
                withContext(dispatcherProvider.io) {
                    store.putString(SettingsKeys.DISTANCE_UNIT, units.distance.serializedName())
                    store.putString(SettingsKeys.ALTITUDE_UNIT, units.altitude.serializedName())
                    store.putString(SettingsKeys.SPEED_UNIT, units.speed.serializedName())
                }
                mutableSettings.value = mutableSettings.value.copy(units = units)
                AppResult.Success(Unit)
            } catch (cancellation: kotlinx.coroutines.CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                AppResult.Failure(Failure.Unexpected)
            }
        }

    override suspend fun updateWarningPreferences(
        warning: WarningPreferences
    ): AppResult<Unit, Failure> {
        val seconds = warning.timeToCollisionWarningSeconds
        if (seconds !in MIN_TIME_TO_COLLISION_WARNING_SECONDS..MAX_TIME_TO_COLLISION_WARNING_SECONDS) {
            return AppResult.Failure(
                Failure.Validation(
                    "Time-to-collision warning must be between " +
                        "$MIN_TIME_TO_COLLISION_WARNING_SECONDS and " +
                        "$MAX_TIME_TO_COLLISION_WARNING_SECONDS seconds."
                )
            )
        }

        return writeMutex.withLock {
            try {
                withContext(dispatcherProvider.io) {
                    store.putInt(SettingsKeys.TIME_TO_COLLISION_WARNING_SECONDS, seconds)
                }
                mutableSettings.value = mutableSettings.value.copy(warning = warning)
                AppResult.Success(Unit)
            } catch (cancellation: kotlinx.coroutines.CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                AppResult.Failure(Failure.Unexpected)
            }
        }
    }

    override suspend fun updateSearchPreferences(
        search: SearchPreferences
    ): AppResult<Unit, Failure> {
        if (search.searchDebounceMillis !in MIN_SEARCH_DEBOUNCE_MILLIS..MAX_SEARCH_DEBOUNCE_MILLIS) {
            return AppResult.Failure(
                Failure.Validation(
                    "Search debounce must be between $MIN_SEARCH_DEBOUNCE_MILLIS and " +
                        "$MAX_SEARCH_DEBOUNCE_MILLIS milliseconds."
                )
            )
        }
        if (search.minimumQueryLength !in MIN_MINIMUM_SEARCH_QUERY_LENGTH..MAX_MINIMUM_SEARCH_QUERY_LENGTH) {
            return AppResult.Failure(
                Failure.Validation(
                    "Minimum search query length must be between $MIN_MINIMUM_SEARCH_QUERY_LENGTH " +
                        "and $MAX_MINIMUM_SEARCH_QUERY_LENGTH characters."
                )
            )
        }

        return writeMutex.withLock {
            try {
                withContext(dispatcherProvider.io) {
                    store.putString(
                        SettingsKeys.SEARCH_DEBOUNCE_MILLIS,
                        search.searchDebounceMillis.toString()
                    )
                    store.putInt(
                        SettingsKeys.MINIMUM_SEARCH_QUERY_LENGTH,
                        search.minimumQueryLength
                    )
                }
                mutableSettings.value = mutableSettings.value.copy(search = search)
                AppResult.Success(Unit)
            } catch (cancellation: kotlinx.coroutines.CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                AppResult.Failure(Failure.Unexpected)
            }
        }
    }

    override suspend fun updateMapPreferences(
        map: MapPreferences
    ): AppResult<Unit, Failure> {
        if (map.maxDynamicZoomSpeedKmh !in MIN_MAX_DYNAMIC_ZOOM_SPEED_KMH..MAX_MAX_DYNAMIC_ZOOM_SPEED_KMH) {
            return AppResult.Failure(
                Failure.Validation(
                    "Max dynamic zoom speed must be between $MIN_MAX_DYNAMIC_ZOOM_SPEED_KMH and " +
                        "$MAX_MAX_DYNAMIC_ZOOM_SPEED_KMH km/h."
                )
            )
        }
        if (map.maxSpeedZoomOutDelta !in MIN_MAX_SPEED_ZOOM_OUT_DELTA..MAX_MAX_SPEED_ZOOM_OUT_DELTA) {
            return AppResult.Failure(
                Failure.Validation(
                    "Max speed zoom-out delta must be between $MIN_MAX_SPEED_ZOOM_OUT_DELTA and " +
                        "$MAX_MAX_SPEED_ZOOM_OUT_DELTA zoom levels."
                )
            )
        }
        if (map.bearingUpdateThresholdDegrees !in MIN_BEARING_UPDATE_THRESHOLD_DEGREES..MAX_BEARING_UPDATE_THRESHOLD_DEGREES) {
            return AppResult.Failure(
                Failure.Validation(
                    "Bearing update threshold must be between $MIN_BEARING_UPDATE_THRESHOLD_DEGREES " +
                        "and $MAX_BEARING_UPDATE_THRESHOLD_DEGREES degrees."
                )
            )
        }

        return writeMutex.withLock {
            try {
                withContext(dispatcherProvider.io) {
                    store.putString(
                        SettingsKeys.MAX_DYNAMIC_ZOOM_SPEED_KMH,
                        map.maxDynamicZoomSpeedKmh.toString()
                    )
                    store.putString(
                        SettingsKeys.MAX_SPEED_ZOOM_OUT_DELTA,
                        map.maxSpeedZoomOutDelta.toString()
                    )
                    store.putString(
                        SettingsKeys.BEARING_UPDATE_THRESHOLD_DEGREES,
                        map.bearingUpdateThresholdDegrees.toString()
                    )
                }
                mutableSettings.value = mutableSettings.value.copy(map = map)
                AppResult.Success(Unit)
            } catch (cancellation: kotlinx.coroutines.CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                AppResult.Failure(Failure.Unexpected)
            }
        }
    }

    /**
     * Loads persisted values, falling back to defaults whenever a value is missing or invalid.
     *
     * Read failures must not surface as exceptions: defaults are emitted instead so the
     * repository remains usable when storage is unavailable.
     */
    private suspend fun loadInitialSettings() {
        val loaded = try {
            withContext(dispatcherProvider.io) { readFromStore() }
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            AppSettings()
        }
        mutableSettings.value = loaded
    }

    private suspend fun readFromStore(): AppSettings {
        val defaults = AppSettings()
        val distance = distanceUnitFromSerialized(store.getString(SettingsKeys.DISTANCE_UNIT))
            ?: defaults.units.distance
        val altitude = altitudeUnitFromSerialized(store.getString(SettingsKeys.ALTITUDE_UNIT))
            ?: defaults.units.altitude
        val speed = speedUnitFromSerialized(store.getString(SettingsKeys.SPEED_UNIT))
            ?: defaults.units.speed

        val storedSeconds = store.getInt(SettingsKeys.TIME_TO_COLLISION_WARNING_SECONDS)
        val seconds = storedSeconds
            ?.takeIf { it in MIN_TIME_TO_COLLISION_WARNING_SECONDS..MAX_TIME_TO_COLLISION_WARNING_SECONDS }
            ?: defaults.warning.timeToCollisionWarningSeconds

        val storedDebounce = store.getString(SettingsKeys.SEARCH_DEBOUNCE_MILLIS)?.toLongOrNull()
        val debounce = storedDebounce
            ?.takeIf { it in MIN_SEARCH_DEBOUNCE_MILLIS..MAX_SEARCH_DEBOUNCE_MILLIS }
            ?: defaults.search.searchDebounceMillis

        val storedMinQuery = store.getInt(SettingsKeys.MINIMUM_SEARCH_QUERY_LENGTH)
        val minQuery = storedMinQuery
            ?.takeIf { it in MIN_MINIMUM_SEARCH_QUERY_LENGTH..MAX_MINIMUM_SEARCH_QUERY_LENGTH }
            ?: defaults.search.minimumQueryLength

        val storedMaxZoomSpeed =
            store.getString(SettingsKeys.MAX_DYNAMIC_ZOOM_SPEED_KMH)?.toDoubleOrNull()
        val maxZoomSpeed = storedMaxZoomSpeed
            ?.takeIf { it in MIN_MAX_DYNAMIC_ZOOM_SPEED_KMH..MAX_MAX_DYNAMIC_ZOOM_SPEED_KMH }
            ?: defaults.map.maxDynamicZoomSpeedKmh

        val storedZoomOutDelta =
            store.getString(SettingsKeys.MAX_SPEED_ZOOM_OUT_DELTA)?.toDoubleOrNull()
        val zoomOutDelta = storedZoomOutDelta
            ?.takeIf { it in MIN_MAX_SPEED_ZOOM_OUT_DELTA..MAX_MAX_SPEED_ZOOM_OUT_DELTA }
            ?: defaults.map.maxSpeedZoomOutDelta

        val storedBearingThreshold =
            store.getString(SettingsKeys.BEARING_UPDATE_THRESHOLD_DEGREES)?.toDoubleOrNull()
        val bearingThreshold = storedBearingThreshold
            ?.takeIf { it in MIN_BEARING_UPDATE_THRESHOLD_DEGREES..MAX_BEARING_UPDATE_THRESHOLD_DEGREES }
            ?: defaults.map.bearingUpdateThresholdDegrees

        return AppSettings(
            units = UnitPreferences(
                distance = distance,
                altitude = altitude,
                speed = speed,
            ),
            warning = WarningPreferences(
                timeToCollisionWarningSeconds = seconds,
            ),
            search = SearchPreferences(
                searchDebounceMillis = debounce,
                minimumQueryLength = minQuery,
            ),
            map = MapPreferences(
                maxDynamicZoomSpeedKmh = maxZoomSpeed,
                maxSpeedZoomOutDelta = zoomOutDelta,
                bearingUpdateThresholdDegrees = bearingThreshold,
            ),
        )
    }
}
