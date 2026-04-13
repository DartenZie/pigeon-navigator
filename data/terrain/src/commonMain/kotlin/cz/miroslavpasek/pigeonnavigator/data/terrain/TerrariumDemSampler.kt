package cz.miroslavpasek.pigeonnavigator.data.terrain

import cz.miroslavpasek.pigeonnavigator.core.util.result.AppResult
import cz.miroslavpasek.pigeonnavigator.domain.failure.Failure

/**
 * Samples terrain elevation from Terrarium-encoded DEM tiles.
 */
interface TerrariumDemSampler {
    /**
     * Returns terrain elevation above mean sea level in meters for the supplied coordinate.
     */
    suspend fun sampleElevationMeters(
        latitude: Double,
        longitude: Double
    ): AppResult<Double, Failure>
}

/**
 * Fallback sampler used when no platform sampler is available.
 */
class UnavailableTerrariumDemSampler : TerrariumDemSampler {
    override suspend fun sampleElevationMeters(
        latitude: Double,
        longitude: Double
    ): AppResult<Double, Failure> = AppResult.Failure(Failure.DataUnavailable)
}

/**
 * Creates a platform-specific Terrarium DEM sampler.
 */
expect fun createTerrariumDemSampler(): TerrariumDemSampler
