package cz.miroslavpasek.pigeonnavigator.data.settings

import cz.miroslavpasek.pigeonnavigator.core.platform.coroutines.DispatcherProvider
import cz.miroslavpasek.pigeonnavigator.core.platform.settings.KeyValueSettingsStore
import cz.miroslavpasek.pigeonnavigator.core.util.result.AppResult
import cz.miroslavpasek.pigeonnavigator.domain.failure.Failure
import cz.miroslavpasek.pigeonnavigator.domain.settings.AltitudeUnit
import cz.miroslavpasek.pigeonnavigator.domain.settings.AppSettings
import cz.miroslavpasek.pigeonnavigator.domain.settings.DEFAULT_TIME_TO_COLLISION_WARNING_SECONDS
import cz.miroslavpasek.pigeonnavigator.domain.settings.DistanceUnit
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
