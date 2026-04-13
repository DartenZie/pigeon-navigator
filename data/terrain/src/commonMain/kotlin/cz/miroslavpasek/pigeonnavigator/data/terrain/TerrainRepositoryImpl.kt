package cz.miroslavpasek.pigeonnavigator.data.terrain

import cz.miroslavpasek.pigeonnavigator.core.util.result.AppResult
import cz.miroslavpasek.pigeonnavigator.domain.failure.Failure
import cz.miroslavpasek.pigeonnavigator.domain.terrain.TerrainRepository

/**
 * Implements [TerrainRepository] by delegating to a terrain elevation data source.
 */
class TerrainRepositoryImpl(
    private val dataSource: TerrainElevationDataSource
) : TerrainRepository {
    override suspend fun sampleTerrainElevationMeters(
        latitude: Double,
        longitude: Double
    ): AppResult<Double, Failure> = dataSource.sampleElevationMeters(latitude, longitude)
}
