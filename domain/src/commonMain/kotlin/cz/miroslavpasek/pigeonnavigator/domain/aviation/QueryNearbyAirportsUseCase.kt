package cz.miroslavpasek.pigeonnavigator.domain.aviation

import cz.miroslavpasek.pigeonnavigator.core.util.result.AppResult
import cz.miroslavpasek.pigeonnavigator.domain.failure.Failure

/**
 * Finds nearby airports around a coordinate.
 */
class QueryNearbyAirportsUseCase(
    private val repository: AviationRepository
) {
    suspend operator fun invoke(
        latitude: Double,
        longitude: Double,
        radiusMeters: Double = AviationRepository.DEFAULT_RADIUS_METERS,
        limit: Int = AviationRepository.DEFAULT_LIMIT
    ): AppResult<List<NearbyAirport>, Failure> =
        repository.nearbyAirports(
            latitude = latitude,
            longitude = longitude,
            radiusMeters = radiusMeters,
            limit = limit
        )
}
