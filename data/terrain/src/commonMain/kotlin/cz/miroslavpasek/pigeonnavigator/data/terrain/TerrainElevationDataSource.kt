package cz.miroslavpasek.pigeonnavigator.data.terrain

import cz.miroslavpasek.pigeonnavigator.core.util.result.AppResult
import cz.miroslavpasek.pigeonnavigator.domain.failure.Failure

/**
 * Supplies terrain elevation samples for geographic coordinates.
 */
interface TerrainElevationDataSource {
    /**
     * Returns elevation above mean sea level in meters for the supplied coordinate.
     *
     * @param latitude Latitude in decimal degrees.
     * @param longitude Longitude in decimal degrees.
     */
    suspend fun sampleElevationMeters(
        latitude: Double,
        longitude: Double
    ): AppResult<Double, Failure>
}
