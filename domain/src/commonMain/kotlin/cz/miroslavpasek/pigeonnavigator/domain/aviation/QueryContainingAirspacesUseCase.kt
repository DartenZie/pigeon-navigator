package cz.miroslavpasek.pigeonnavigator.domain.aviation

import cz.miroslavpasek.pigeonnavigator.core.util.result.AppResult
import cz.miroslavpasek.pigeonnavigator.domain.failure.Failure

/**
 * Finds airspaces that contain a coordinate.
 */
class QueryContainingAirspacesUseCase(
    private val repository: AviationRepository
) {
    suspend operator fun invoke(
        latitude: Double,
        longitude: Double
    ): AppResult<List<Airspace>, Failure> =
        repository.containingAirspaces(latitude = latitude, longitude = longitude)
}
