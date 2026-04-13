package cz.miroslavpasek.pigeonnavigator.data.terrain

import cz.miroslavpasek.pigeonnavigator.core.util.result.AppResult
import cz.miroslavpasek.pigeonnavigator.domain.failure.Failure

/**
 * Wraps a terrain sampler with manifest-defined geographic bounds checks.
 *
 * Coordinates outside [bounds] return [Failure.OutOfCoverage] without calling [sampler].
 */
class ManifestBoundedTerrainDataSource(
    private val sampler: TerrariumDemSampler,
    private val bounds: TerrainBounds = TerrainBounds(
        minLongitude = 12.09,
        minLatitude = 48.55,
        maxLongitude = 18.87,
        maxLatitude = 51.06
    )
) : TerrainElevationDataSource {

    /**
     * Returns elevation for [latitude]/[longitude] when inside coverage bounds.
     */
    override suspend fun sampleElevationMeters(
        latitude: Double,
        longitude: Double
    ): AppResult<Double, Failure> {
        if (!bounds.contains(latitude = latitude, longitude = longitude)) {
            return AppResult.Failure(Failure.OutOfCoverage)
        }

        return sampler.sampleElevationMeters(latitude = latitude, longitude = longitude)
    }
}

/**
 * Defines a rectangular WGS84 coverage area for terrain data.
 */
data class TerrainBounds(
    val minLongitude: Double,
    val minLatitude: Double,
    val maxLongitude: Double,
    val maxLatitude: Double
) {
    /**
     * Returns `true` when the coordinate lies within inclusive latitude and longitude bounds.
     */
    fun contains(latitude: Double, longitude: Double): Boolean {
        return latitude in minLatitude..maxLatitude && longitude in minLongitude..maxLongitude
    }
}
