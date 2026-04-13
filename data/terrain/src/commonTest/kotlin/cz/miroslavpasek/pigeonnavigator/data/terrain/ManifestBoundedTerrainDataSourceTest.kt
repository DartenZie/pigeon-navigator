package cz.miroslavpasek.pigeonnavigator.data.terrain

import cz.miroslavpasek.pigeonnavigator.core.util.result.AppResult
import cz.miroslavpasek.pigeonnavigator.domain.failure.Failure
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ManifestBoundedTerrainDataSourceTest {

    @Test
    fun delegatesToSamplerInsideBounds() = runBlocking {
        val dataSource = ManifestBoundedTerrainDataSource(
            sampler = object : TerrariumDemSampler {
                override suspend fun sampleElevationMeters(
                    latitude: Double,
                    longitude: Double
                ): AppResult<Double, Failure> = AppResult.Success(777.0)
            },
            bounds = TerrainBounds(10.0, 40.0, 20.0, 60.0)
        )

        val result = dataSource.sampleElevationMeters(latitude = 50.0, longitude = 15.0)
        assertEquals(777.0, assertIs<AppResult.Success<Double>>(result).value)
    }

    @Test
    fun returnsOutOfCoverageOutsideBounds() = runBlocking {
        val dataSource = ManifestBoundedTerrainDataSource(
            sampler = UnavailableTerrariumDemSampler(),
            bounds = TerrainBounds(10.0, 40.0, 20.0, 60.0)
        )

        val result = dataSource.sampleElevationMeters(latitude = 61.0, longitude = 15.0)
        assertEquals(Failure.OutOfCoverage, assertIs<AppResult.Failure<Failure>>(result).error)
    }
}
