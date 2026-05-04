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
    fun includesTerrainElevationAtCurrentLocation() = runBlocking {
        val useCase = DetectTerrainConflictUseCase(
            terrainRepository = FakeTerrainRepository { latitude, _ ->
                if (latitude == 50.0) 420.0 else 100.0
            }
        )

        val result = useCase(
            snapshot = AircraftSnapshot(
                latitude = 50.0,
                longitude = 14.0,
                altitudeMeters = 1_000.0,
                speedMetersPerSecond = 40.0,
                bearingDegrees = 0.0
            )
        )

        val prediction = assertIs<AppResult.Success<TerrainConflictPrediction>>(result).value
        assertEquals(420.0, prediction.currentTerrainElevationMeters)
    }

    @Test
    fun includesCurrentTerrainFailureWhenOnlyAheadSamplesAreAvailable() = runBlocking {
        val useCase = DetectTerrainConflictUseCase(
            terrainRepository = object : TerrainRepository {
                override suspend fun sampleTerrainElevationMeters(
                    latitude: Double,
                    longitude: Double
                ): AppResult<Double, Failure> {
                    return if (latitude == 50.0 && longitude == 14.0) {
                        AppResult.Failure(Failure.OutOfCoverage)
                    } else {
                        AppResult.Success(100.0)
                    }
                }
            }
        )

        val result = useCase(
            snapshot = AircraftSnapshot(
                latitude = 50.0,
                longitude = 14.0,
                altitudeMeters = 1_000.0,
                speedMetersPerSecond = 40.0,
                bearingDegrees = 0.0
            )
        )

        val prediction = assertIs<AppResult.Success<TerrainConflictPrediction>>(result).value
        assertEquals(null, prediction.currentTerrainElevationMeters)
        assertEquals(Failure.OutOfCoverage, prediction.currentTerrainFailure)
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
    fun preservesCurrentTerrainDiagnosticWhenNoSampleIsAvailable() = runBlocking {
        val diagnosticFailure = Failure.DataUnavailableReason("PNG_DECODE")
        val useCase = DetectTerrainConflictUseCase(
            terrainRepository = object : TerrainRepository {
                override suspend fun sampleTerrainElevationMeters(
                    latitude: Double,
                    longitude: Double
                ): AppResult<Double, Failure> = AppResult.Failure(diagnosticFailure)
            }
        )

        val result = useCase(
            snapshot = AircraftSnapshot(
                latitude = 50.0000,
                longitude = 14.0000,
                altitudeMeters = 1500.0,
                speedMetersPerSecond = 50.0,
                bearingDegrees = 90.0
            )
        )

        val error = assertIs<AppResult.Failure<Failure>>(result).error
        assertEquals(diagnosticFailure, error)
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

    @Test
    fun returnsWarningWhenTerrainViolatesClearanceMarginBelowAmslAltitude() = runBlocking {
        val useCase = DetectTerrainConflictUseCase(
            terrainRepository = FakeTerrainRepository { _, _ -> 850.0 }
        )

        val result = useCase(
            snapshot = AircraftSnapshot(
                latitude = 50.0,
                longitude = 14.0,
                altitudeMeters = 1_000.0,
                speedMetersPerSecond = 40.0,
                bearingDegrees = 0.0
            ),
            parameters = TerrainConflictParameters(
                safetyMarginMeters = 100.0,
                warningClearanceMeters = 60.0,
                cautionClearanceMeters = 150.0
            )
        )

        val prediction = assertIs<AppResult.Success<TerrainConflictPrediction>>(result).value
        assertFalse(prediction.hasConflict)
        assertEquals(TerrainWarningLevel.Warning, prediction.warningLevel)
        assertEquals(50.0, prediction.minClearanceMeters)
    }

    @Test
    fun marksNearConflictAtMinusFiftyMetersVerticalDelta() = runBlocking {
        val useCase = DetectTerrainConflictUseCase(
            terrainRepository = FakeTerrainRepository { _, _ -> 950.0 }
        )

        val result = useCase(
            snapshot = AircraftSnapshot(
                latitude = 50.0,
                longitude = 14.0,
                altitudeMeters = 1_000.0,
                speedMetersPerSecond = 40.0,
                bearingDegrees = 0.0
            )
        )

        val prediction = assertIs<AppResult.Success<TerrainConflictPrediction>>(result).value
        assertFalse(prediction.hasConflict)
        assertTrue(prediction.hazardSamples.isNotEmpty())
        assertTrue(prediction.hazardSamples.all { it.level == TerrainHazardLevel.NearConflict })
    }

    @Test
    fun doesNotMarkNearConflictWhenTerrainIsBelowMinusFiftyMetersBand() = runBlocking {
        val useCase = DetectTerrainConflictUseCase(
            terrainRepository = FakeTerrainRepository { _, _ -> 949.0 }
        )

        val result = useCase(
            snapshot = AircraftSnapshot(
                latitude = 50.0,
                longitude = 14.0,
                altitudeMeters = 1_000.0,
                speedMetersPerSecond = 40.0,
                bearingDegrees = 0.0
            )
        )

        val prediction = assertIs<AppResult.Success<TerrainConflictPrediction>>(result).value
        assertTrue(prediction.hazardSamples.none { it.level == TerrainHazardLevel.NearConflict })
    }

    @Test
    fun marksConflictWhenTerrainReachesAircraftAltitude() = runBlocking {
        val useCase = DetectTerrainConflictUseCase(
            terrainRepository = FakeTerrainRepository { _, _ -> 1_000.0 }
        )

        val result = useCase(
            snapshot = AircraftSnapshot(
                latitude = 50.0,
                longitude = 14.0,
                altitudeMeters = 1_000.0,
                speedMetersPerSecond = 40.0,
                bearingDegrees = 0.0
            )
        )

        val prediction = assertIs<AppResult.Success<TerrainConflictPrediction>>(result).value
        assertTrue(prediction.hasConflict)
        assertTrue(prediction.hazardSamples.any { it.level == TerrainHazardLevel.Conflict })
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
