package cz.miroslavpasek.pigeonnavigator.domain.terrain

import cz.miroslavpasek.pigeonnavigator.core.util.result.AppResult
import cz.miroslavpasek.pigeonnavigator.domain.failure.Failure

/**
 * Provides terrain elevation samples for geographic coordinates.
 */
interface TerrainRepository {
    /**
     * Returns terrain elevation above mean sea level in meters at the provided coordinate.
     *
     * @param latitude Latitude in decimal degrees.
     * @param longitude Longitude in decimal degrees.
     * @return [AppResult.Success] with elevation in meters, or [AppResult.Failure] with a domain failure.
     */
    suspend fun sampleTerrainElevationMeters(
        latitude: Double,
        longitude: Double
    ): AppResult<Double, Failure>
}
