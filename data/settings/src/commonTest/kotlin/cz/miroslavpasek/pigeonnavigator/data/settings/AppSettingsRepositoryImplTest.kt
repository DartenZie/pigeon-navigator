package cz.miroslavpasek.pigeonnavigator.data.settings

import cz.miroslavpasek.pigeonnavigator.core.platform.coroutines.DispatcherProvider
import cz.miroslavpasek.pigeonnavigator.core.platform.settings.KeyValueSettingsStore
import cz.miroslavpasek.pigeonnavigator.core.util.result.AppResult
import cz.miroslavpasek.pigeonnavigator.domain.failure.Failure
import cz.miroslavpasek.pigeonnavigator.domain.settings.AltitudeUnit
import cz.miroslavpasek.pigeonnavigator.domain.settings.AppSettings
import cz.miroslavpasek.pigeonnavigator.domain.settings.DEFAULT_TIME_TO_COLLISION_WARNING_SECONDS
import cz.miroslavpasek.pigeonnavigator.domain.settings.DistanceUnit
import cz.miroslavpasek.pigeonnavigator.domain.settings.MapPreferences
import cz.miroslavpasek.pigeonnavigator.domain.settings.SearchPreferences
import cz.miroslavpasek.pigeonnavigator.domain.settings.SpeedUnit
import cz.miroslavpasek.pigeonnavigator.domain.settings.UnitPreferences
import cz.miroslavpasek.pigeonnavigator.domain.settings.WarningPreferences
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AppSettingsRepositoryImplTest {

    @Test
    fun emitsDefaultsWhenStorageEmpty() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val store = InMemoryKeyValueSettingsStore()
        val repository = AppSettingsRepositoryImpl(
            store = store,
            dispatcherProvider = TestDispatcherProvider(dispatcher),
            scope = CoroutineScope(SupervisorJob() + dispatcher),
        )

        advanceUntilIdle()

        assertEquals(AppSettings(), repository.settings.value)
    }

    @Test
    fun loadsPersistedUnitsAndWarning() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val store = InMemoryKeyValueSettingsStore(
            strings = mutableMapOf(
                "app_settings.units.distance" to "kilometers",
                "app_settings.units.altitude" to "meters",
                "app_settings.units.speed" to "meters_per_second",
            ),
            ints = mutableMapOf(
                "app_settings.warning.time_to_collision_seconds" to 120,
            ),
        )
        val repository = AppSettingsRepositoryImpl(
            store = store,
            dispatcherProvider = TestDispatcherProvider(dispatcher),
            scope = CoroutineScope(SupervisorJob() + dispatcher),
        )

        advanceUntilIdle()

        val loaded = repository.settings.value
        assertEquals(DistanceUnit.Kilometers, loaded.units.distance)
        assertEquals(AltitudeUnit.Meters, loaded.units.altitude)
        assertEquals(SpeedUnit.MetersPerSecond, loaded.units.speed)
        assertEquals(120, loaded.warning.timeToCollisionWarningSeconds)
    }

    @Test
    fun fallsBackToDefaultsForInvalidPersistedEnumValues() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val store = InMemoryKeyValueSettingsStore(
            strings = mutableMapOf(
                "app_settings.units.distance" to "parsecs",
                "app_settings.units.altitude" to "leagues",
                "app_settings.units.speed" to "warp",
            ),
        )
        val repository = AppSettingsRepositoryImpl(
            store = store,
            dispatcherProvider = TestDispatcherProvider(dispatcher),
            scope = CoroutineScope(SupervisorJob() + dispatcher),
        )

        advanceUntilIdle()

        assertEquals(UnitPreferences(), repository.settings.value.units)
    }

    @Test
    fun fallsBackToDefaultsForOutOfRangePersistedWarning() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val store = InMemoryKeyValueSettingsStore(
            ints = mutableMapOf(
                "app_settings.warning.time_to_collision_seconds" to 9999,
            ),
        )
        val repository = AppSettingsRepositoryImpl(
            store = store,
            dispatcherProvider = TestDispatcherProvider(dispatcher),
            scope = CoroutineScope(SupervisorJob() + dispatcher),
        )

        advanceUntilIdle()

        assertEquals(
            DEFAULT_TIME_TO_COLLISION_WARNING_SECONDS,
            repository.settings.value.warning.timeToCollisionWarningSeconds
        )
    }

    @Test
    fun persistsAndEmitsUpdatedUnits() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val store = InMemoryKeyValueSettingsStore()
        val repository = AppSettingsRepositoryImpl(
            store = store,
            dispatcherProvider = TestDispatcherProvider(dispatcher),
            scope = CoroutineScope(SupervisorJob() + dispatcher),
        )
        advanceUntilIdle()

        val newUnits = UnitPreferences(
            distance = DistanceUnit.Miles,
            altitude = AltitudeUnit.Meters,
            speed = SpeedUnit.MilesPerHour,
        )
        val result = repository.updateUnits(newUnits)
        advanceUntilIdle()

        assertTrue(result is AppResult.Success)
        assertEquals(newUnits, repository.settings.value.units)
        assertEquals("miles", store.strings["app_settings.units.distance"])
        assertEquals("meters", store.strings["app_settings.units.altitude"])
        assertEquals("miles_per_hour", store.strings["app_settings.units.speed"])
    }

    @Test
    fun persistsAndEmitsUpdatedWarningPreferences() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val store = InMemoryKeyValueSettingsStore()
        val repository = AppSettingsRepositoryImpl(
            store = store,
            dispatcherProvider = TestDispatcherProvider(dispatcher),
            scope = CoroutineScope(SupervisorJob() + dispatcher),
        )
        advanceUntilIdle()

        val result = repository.updateWarningPreferences(
            WarningPreferences(timeToCollisionWarningSeconds = 45)
        )
        advanceUntilIdle()

        assertTrue(result is AppResult.Success)
        assertEquals(45, repository.settings.value.warning.timeToCollisionWarningSeconds)
        assertEquals(45, store.ints["app_settings.warning.time_to_collision_seconds"])
    }

    @Test
    fun rejectsOutOfRangeWarningUpdateWithValidationFailure() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val store = InMemoryKeyValueSettingsStore()
        val repository = AppSettingsRepositoryImpl(
            store = store,
            dispatcherProvider = TestDispatcherProvider(dispatcher),
            scope = CoroutineScope(SupervisorJob() + dispatcher),
        )
        advanceUntilIdle()

        val tooLow = repository.updateWarningPreferences(
            WarningPreferences(timeToCollisionWarningSeconds = 0)
        )
        val tooHigh = repository.updateWarningPreferences(
            WarningPreferences(timeToCollisionWarningSeconds = 9999)
        )

        assertTrue(tooLow is AppResult.Failure)
        assertTrue(tooLow.error is Failure.Validation)
        assertTrue(tooHigh is AppResult.Failure)
        assertTrue(tooHigh.error is Failure.Validation)

        // Invalid updates must not have been persisted.
        assertEquals(null, store.ints["app_settings.warning.time_to_collision_seconds"])
        assertEquals(
            DEFAULT_TIME_TO_COLLISION_WARNING_SECONDS,
            repository.settings.value.warning.timeToCollisionWarningSeconds
        )
    }

    @Test
    fun loadsPersistedSearchAndMapPreferences() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val store = InMemoryKeyValueSettingsStore(
            strings = mutableMapOf(
                "app_settings.search.debounce_millis" to "450",
                "app_settings.map.max_dynamic_zoom_speed_kmh" to "275.5",
                "app_settings.map.max_speed_zoom_out_delta" to "3.25",
                "app_settings.map.bearing_update_threshold_degrees" to "6.0",
            ),
            ints = mutableMapOf(
                "app_settings.search.minimum_query_length" to 3,
            ),
        )
        val repository = AppSettingsRepositoryImpl(
            store = store,
            dispatcherProvider = TestDispatcherProvider(dispatcher),
            scope = CoroutineScope(SupervisorJob() + dispatcher),
        )
        advanceUntilIdle()

        val loaded = repository.settings.value
        assertEquals(450L, loaded.search.searchDebounceMillis)
        assertEquals(3, loaded.search.minimumQueryLength)
        assertEquals(275.5, loaded.map.maxDynamicZoomSpeedKmh)
        assertEquals(3.25, loaded.map.maxSpeedZoomOutDelta)
        assertEquals(6.0, loaded.map.bearingUpdateThresholdDegrees)
    }

    @Test
    fun fallsBackToDefaultsForOutOfRangeOrUnparsableSearchAndMapValues() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val store = InMemoryKeyValueSettingsStore(
            strings = mutableMapOf(
                "app_settings.search.debounce_millis" to "not-a-number",
                "app_settings.map.max_dynamic_zoom_speed_kmh" to "999999",
                "app_settings.map.max_speed_zoom_out_delta" to "not-a-double",
                "app_settings.map.bearing_update_threshold_degrees" to "-5",
            ),
            ints = mutableMapOf(
                "app_settings.search.minimum_query_length" to 99,
            ),
        )
        val repository = AppSettingsRepositoryImpl(
            store = store,
            dispatcherProvider = TestDispatcherProvider(dispatcher),
            scope = CoroutineScope(SupervisorJob() + dispatcher),
        )
        advanceUntilIdle()

        val loaded = repository.settings.value
        assertEquals(SearchPreferences(), loaded.search)
        assertEquals(MapPreferences(), loaded.map)
    }

    @Test
    fun persistsAndEmitsUpdatedSearchPreferences() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val store = InMemoryKeyValueSettingsStore()
        val repository = AppSettingsRepositoryImpl(
            store = store,
            dispatcherProvider = TestDispatcherProvider(dispatcher),
            scope = CoroutineScope(SupervisorJob() + dispatcher),
        )
        advanceUntilIdle()

        val newSearch = SearchPreferences(searchDebounceMillis = 500L, minimumQueryLength = 4)
        val result = repository.updateSearchPreferences(newSearch)
        advanceUntilIdle()

        assertTrue(result is AppResult.Success)
        assertEquals(newSearch, repository.settings.value.search)
        assertEquals("500", store.strings["app_settings.search.debounce_millis"])
        assertEquals(4, store.ints["app_settings.search.minimum_query_length"])
    }

    @Test
    fun persistsAndEmitsUpdatedMapPreferences() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val store = InMemoryKeyValueSettingsStore()
        val repository = AppSettingsRepositoryImpl(
            store = store,
            dispatcherProvider = TestDispatcherProvider(dispatcher),
            scope = CoroutineScope(SupervisorJob() + dispatcher),
        )
        advanceUntilIdle()

        val newMap = MapPreferences(
            maxDynamicZoomSpeedKmh = 220.0,
            maxSpeedZoomOutDelta = 1.75,
            bearingUpdateThresholdDegrees = 7.5,
        )
        val result = repository.updateMapPreferences(newMap)
        advanceUntilIdle()

        assertTrue(result is AppResult.Success)
        assertEquals(newMap, repository.settings.value.map)
        assertEquals("220.0", store.strings["app_settings.map.max_dynamic_zoom_speed_kmh"])
        assertEquals("1.75", store.strings["app_settings.map.max_speed_zoom_out_delta"])
        assertEquals("7.5", store.strings["app_settings.map.bearing_update_threshold_degrees"])
    }

    @Test
    fun rejectsOutOfRangeSearchAndMapUpdates() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val store = InMemoryKeyValueSettingsStore()
        val repository = AppSettingsRepositoryImpl(
            store = store,
            dispatcherProvider = TestDispatcherProvider(dispatcher),
            scope = CoroutineScope(SupervisorJob() + dispatcher),
        )
        advanceUntilIdle()

        val invalidSearch = repository.updateSearchPreferences(
            SearchPreferences(searchDebounceMillis = -10L, minimumQueryLength = 0)
        )
        val invalidMap = repository.updateMapPreferences(
            MapPreferences(
                maxDynamicZoomSpeedKmh = 10.0,
                maxSpeedZoomOutDelta = 100.0,
                bearingUpdateThresholdDegrees = 999.0,
            )
        )

        assertTrue(invalidSearch is AppResult.Failure)
        assertTrue(invalidSearch.error is Failure.Validation)
        assertTrue(invalidMap is AppResult.Failure)
        assertTrue(invalidMap.error is Failure.Validation)

        // No partial writes should have leaked through.
        assertEquals(null, store.strings["app_settings.search.debounce_millis"])
        assertEquals(null, store.ints["app_settings.search.minimum_query_length"])
        assertEquals(null, store.strings["app_settings.map.max_dynamic_zoom_speed_kmh"])
        assertEquals(SearchPreferences(), repository.settings.value.search)
        assertEquals(MapPreferences(), repository.settings.value.map)
    }
}

private class TestDispatcherProvider(
    dispatcher: CoroutineDispatcher
) : DispatcherProvider {
    override val main: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
    override val default: CoroutineDispatcher = dispatcher
}

private class InMemoryKeyValueSettingsStore(
    val strings: MutableMap<String, String> = mutableMapOf(),
    val ints: MutableMap<String, Int> = mutableMapOf(),
) : KeyValueSettingsStore {
    override suspend fun getString(key: String): String? = strings[key]
    override suspend fun putString(key: String, value: String) {
        strings[key] = value
    }

    override suspend fun getInt(key: String): Int? = ints[key]
    override suspend fun putInt(key: String, value: Int) {
        ints[key] = value
    }
}
