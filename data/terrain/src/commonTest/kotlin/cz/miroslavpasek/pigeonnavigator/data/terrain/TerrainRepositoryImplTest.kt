package cz.miroslavpasek.pigeonnavigator.data.terrain

import cz.miroslavpasek.pigeonnavigator.core.util.result.AppResult
import cz.miroslavpasek.pigeonnavigator.domain.failure.Failure
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TerrainRepositoryImplTest {

    @Test
    fun delegatesToDataSource() = runBlocking {
        val repository = TerrainRepositoryImpl(
            dataSource = object : TerrainElevationDataSource {
                override suspend fun sampleElevationMeters(
                    latitude: Double,
                    longitude: Double
                ): AppResult<Double, Failure> {
                    return AppResult.Success(latitude + longitude)
                }
            }
        )

        val result = repository.sampleTerrainElevationMeters(50.0, 14.0)
        assertEquals(64.0, assertIs<AppResult.Success<Double>>(result).value)
    }

    @Test
    fun preservesFailureFromDataSource() = runBlocking {
        val repository = TerrainRepositoryImpl(
            dataSource = object : TerrainElevationDataSource {
                override suspend fun sampleElevationMeters(
                    latitude: Double,
                    longitude: Double
                ): AppResult<Double, Failure> {
                    return AppResult.Failure(Failure.DataUnavailable)
                }
            }
        )

        val result = repository.sampleTerrainElevationMeters(50.0, 14.0)
        assertEquals(Failure.DataUnavailable, assertIs<AppResult.Failure<Failure>>(result).error)
    }
}
