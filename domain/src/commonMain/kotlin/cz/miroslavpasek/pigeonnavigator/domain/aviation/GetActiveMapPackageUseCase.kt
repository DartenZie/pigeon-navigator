package cz.miroslavpasek.pigeonnavigator.domain.aviation

import cz.miroslavpasek.pigeonnavigator.core.util.result.AppResult
import cz.miroslavpasek.pigeonnavigator.domain.failure.Failure

/**
 * Returns active package PMTiles file paths.
 */
class GetActiveMapPackageUseCase(
    private val repository: AviationRepository
) {
    suspend operator fun invoke(): AppResult<ActiveMapPackage, Failure> =
        repository.getActiveMapPackage()
}
