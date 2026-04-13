package cz.miroslavpasek.pigeonnavigator.feature.terrainwarning.internal

import cz.miroslavpasek.pigeonnavigator.core.platform.coroutines.DispatcherProvider
import cz.miroslavpasek.pigeonnavigator.core.util.result.AppResult
import cz.miroslavpasek.pigeonnavigator.domain.failure.Failure
import cz.miroslavpasek.pigeonnavigator.domain.terrain.AircraftSnapshot
import cz.miroslavpasek.pigeonnavigator.domain.terrain.DetectTerrainConflictUseCase
import cz.miroslavpasek.pigeonnavigator.domain.terrain.TerrainRepository
import cz.miroslavpasek.pigeonnavigator.domain.terrain.TerrainWarningLevel
import cz.miroslavpasek.pigeonnavigator.feature.terrainwarning.presentation.TerrainWarningEffect
import cz.miroslavpasek.pigeonnavigator.feature.terrainwarning.presentation.TerrainWarningIntent
import cz.miroslavpasek.pigeonnavigator.feature.terrainwarning.presentation.TerrainWarningReducer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RealTerrainWarningStoreTest {

    @Test
    fun emitsWarningEffectWhenConflictAppears() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val store = RealTerrainWarningStore(
            detectTerrainConflictUseCase = DetectTerrainConflictUseCase(
                terrainRepository = FakeTerrainRepository { latitude, _ ->
                    if (latitude > 50.0015) 2_200.0 else 300.0
                }
            ),
            reducer = TerrainWarningReducer(),
            dispatcherProvider = TestDispatcherProvider(dispatcher)
        )

        val effectDeferred = async { store.effects.first() }

        store.send(
            TerrainWarningIntent.LocationUpdated(
                AircraftSnapshot(
                    latitude = 50.0,
                    longitude = 14.0,
                    altitudeMeters = 1_000.0,
                    speedMetersPerSecond = 60.0,
                    bearingDegrees = 0.0
                )
            )
        )

        advanceUntilIdle()

        val effect = effectDeferred.await()
        assertEquals(TerrainWarningEffect.TriggerWarning::class, effect::class)
        assertEquals(TerrainWarningLevel.Warning, store.state.value.warningLevel)
        store.close()
    }

    @Test
    fun storesFailureMessageWhenDataUnavailable() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val store = RealTerrainWarningStore(
            detectTerrainConflictUseCase = DetectTerrainConflictUseCase(
                terrainRepository = object : TerrainRepository {
                    override suspend fun sampleTerrainElevationMeters(
                        latitude: Double,
                        longitude: Double
                    ): AppResult<Double, Failure> = AppResult.Failure(Failure.DataUnavailable)
                }
            ),
            reducer = TerrainWarningReducer(),
            dispatcherProvider = TestDispatcherProvider(dispatcher)
        )

        store.send(
            TerrainWarningIntent.LocationUpdated(
                AircraftSnapshot(
                    latitude = 50.0,
                    longitude = 14.0,
                    altitudeMeters = 1_000.0,
                    speedMetersPerSecond = 40.0,
                    bearingDegrees = 0.0
                )
            )
        )

        advanceUntilIdle()

        assertEquals("Terrain data unavailable", store.state.value.lastErrorMessage)
        store.close()
    }

    @Test
    fun skipsRecomputeForSmallLocationChanges() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val repository = CountingTerrainRepository { _, _ -> 200.0 }
        val store = RealTerrainWarningStore(
            detectTerrainConflictUseCase = DetectTerrainConflictUseCase(terrainRepository = repository),
            reducer = TerrainWarningReducer(),
            dispatcherProvider = TestDispatcherProvider(dispatcher)
        )

        store.send(
            TerrainWarningIntent.LocationUpdated(
                AircraftSnapshot(
                    latitude = 50.0,
                    longitude = 14.0,
                    altitudeMeters = 1_000.0,
                    speedMetersPerSecond = 40.0,
                    bearingDegrees = 0.0
                )
            )
        )
        advanceUntilIdle()
        val callsAfterFirstUpdate = repository.calls

        store.send(
            TerrainWarningIntent.LocationUpdated(
                AircraftSnapshot(
                    latitude = 50.00001,
                    longitude = 14.00001,
                    altitudeMeters = 1_005.0,
                    speedMetersPerSecond = 40.0,
                    bearingDegrees = 1.5
                )
            )
        )
        advanceUntilIdle()

        assertEquals(callsAfterFirstUpdate, repository.calls)
        store.close()
    }

    @Test
    fun emitsClearWarningWhenConflictResolves() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val repository = SwitchingTerrainRepository()
        val store = RealTerrainWarningStore(
            detectTerrainConflictUseCase = DetectTerrainConflictUseCase(terrainRepository = repository),
            reducer = TerrainWarningReducer(),
            dispatcherProvider = TestDispatcherProvider(dispatcher)
        )

        val firstEffectDeferred = async { store.effects.first() }
        store.send(
            TerrainWarningIntent.LocationUpdated(
                AircraftSnapshot(
                    latitude = 50.0,
                    longitude = 14.0,
                    altitudeMeters = 1_000.0,
                    speedMetersPerSecond = 60.0,
                    bearingDegrees = 0.0
                )
            )
        )
        advanceUntilIdle()
        assertEquals(TerrainWarningEffect.TriggerWarning::class, firstEffectDeferred.await()::class)

        repository.conflict = false
        val secondEffectDeferred = async { store.effects.first() }
        store.send(
            TerrainWarningIntent.LocationUpdated(
                AircraftSnapshot(
                    latitude = 50.001,
                    longitude = 14.0,
                    altitudeMeters = 1_000.0,
                    speedMetersPerSecond = 60.0,
                    bearingDegrees = 6.0
                )
            )
        )
        advanceUntilIdle()

        assertEquals(TerrainWarningEffect.ClearWarning::class, secondEffectDeferred.await()::class)
        assertEquals(TerrainWarningLevel.None, store.state.value.warningLevel)
        store.close()
    }
}

private class TestDispatcherProvider(
    dispatcher: CoroutineDispatcher
) : DispatcherProvider {
    override val main: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
    override val default: CoroutineDispatcher = dispatcher
}

private class FakeTerrainRepository(
    private val provider: (Double, Double) -> Double
) : TerrainRepository {
    override suspend fun sampleTerrainElevationMeters(
        latitude: Double,
        longitude: Double
    ): AppResult<Double, Failure> {
        return AppResult.Success(provider(latitude, longitude))
    }
}

private class CountingTerrainRepository(
    private val provider: (Double, Double) -> Double
) : TerrainRepository {
    var calls: Int = 0

    override suspend fun sampleTerrainElevationMeters(
        latitude: Double,
        longitude: Double
    ): AppResult<Double, Failure> {
        calls += 1
        return AppResult.Success(provider(latitude, longitude))
    }
}

private class SwitchingTerrainRepository : TerrainRepository {
    var conflict: Boolean = true

    override suspend fun sampleTerrainElevationMeters(
        latitude: Double,
        longitude: Double
    ): AppResult<Double, Failure> {
        val elevation = if (conflict && latitude >= 50.001) 2_000.0 else 100.0
        return AppResult.Success(elevation)
    }
}
