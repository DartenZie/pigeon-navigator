package cz.miroslavpasek.pigeonnavigator.domain.aviation

import cz.miroslavpasek.pigeonnavigator.core.util.result.AppResult
import cz.miroslavpasek.pigeonnavigator.domain.failure.Failure

/**
 * Installs and activates an aviation package.
 */
class InstallAviationPackageUseCase(
    private val repository: AviationRepository
) {
    suspend operator fun invoke(source: AviationPackageSource): AppResult<Unit, Failure> =
        repository.installPackage(source)
}
