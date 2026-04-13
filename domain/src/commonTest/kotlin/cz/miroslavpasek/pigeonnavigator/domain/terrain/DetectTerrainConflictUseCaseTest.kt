package cz.miroslavpasek.pigeonnavigator.domain.terrain

import cz.miroslavpasek.pigeonnavigator.core.util.result.AppResult
import cz.miroslavpasek.pigeonnavigator.domain.failure.Failure
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DetectTerrainConflictUseCaseTest {

    @Test
    fun returnsWarningWhenCollisionDetectedAhead() = runBlocking {
        val useCase = DetectTerrainConflictUseCase(
            terrainRepository = FakeTerrainRepository { latitude, _ ->
                if (latitude >= 50.0100) 1300.0 else 450.0
            }
        )

        val result = useCase(
            snapshot = AircraftSnapshot(
                latitude = 50.0000,
                longitude = 14.4000,
                altitudeMeters = 1200.0,
                speedMetersPerSecond = 65.0,
                bearingDegrees = 0.0
            )
        )

        val prediction = assertIs<AppResult.Success<TerrainConflictPrediction>>(result).value
        assertTrue(prediction.hasConflict)
        assertEquals(TerrainWarningLevel.Warning, prediction.warningLevel)
        assertTrue((prediction.distanceToImpactMeters ?: 0.0) > 0.0)
    }

    @Test
    fun returnsOutOfCoverageWhenNoSampleIsAvailable() = runBlocking {
        val useCase = DetectTerrainConflictUseCase(
            terrainRepository = object : TerrainRepository {
                override suspend fun sampleTerrainElevationMeters(
                    latitude: Double,
                    longitude: Double
                ): AppResult<Double, Failure> = AppResult.Failure(Failure.OutOfCoverage)
            }
        )

        val result = useCase(
            snapshot = AircraftSnapshot(
                latitude = 46.0000,
                longitude = 10.0000,
                altitudeMeters = 1500.0,
                speedMetersPerSecond = 50.0,
                bearingDegrees = 90.0
            )
        )

        val error = assertIs<AppResult.Failure<Failure>>(result).error
        assertEquals(Failure.OutOfCoverage, error)
    }

    @Test
    fun returnsNoConflictWhenClearanceIsSafe() = runBlocking {
        val useCase = DetectTerrainConflictUseCase(
            terrainRepository = FakeTerrainRepository { _, _ -> 100.0 }
        )

        val result = useCase(
            snapshot = AircraftSnapshot(
                latitude = 50.0,
                longitude = 14.0,
                altitudeMeters = 1_500.0,
                speedMetersPerSecond = 55.0,
                bearingDegrees = 45.0
            )
        )

        val prediction = assertIs<AppResult.Success<TerrainConflictPrediction>>(result).value
        assertFalse(prediction.hasConflict)
        assertEquals(TerrainWarningLevel.None, prediction.warningLevel)
    }
}

private class FakeTerrainRepository(
    private val provider: (Double, Double) -> Double
) : TerrainRepository {
    override suspend fun sampleTerrainElevationMeters(
        latitude: Double,
        longitude: Double
    ): AppResult<Double, Failure> = AppResult.Success(provider(latitude, longitude))
}
