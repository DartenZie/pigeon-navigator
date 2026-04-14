package cz.miroslavpasek.pigeonnavigator.data.aviation

import cz.miroslavpasek.pigeonnavigator.core.util.result.AppResult
import cz.miroslavpasek.pigeonnavigator.domain.aviation.AviationPackageSource
import cz.miroslavpasek.pigeonnavigator.domain.aviation.AviationRepository

/**
 * Installs bundled bootstrap package when no active package is present.
 */
class AviationPackageBootstrapper(
    private val repository: AviationRepository
) {
    suspend fun ensureInstalledFromAsset(assetPath: String = DEFAULT_BOOTSTRAP_ASSET_PATH) {
        val active = repository.getActiveMapPackage()
        if (active is AppResult.Success) {
            return
        }

        repository.installPackage(AviationPackageSource.Asset(assetPath))
    }

    private companion object {
        const val DEFAULT_BOOTSTRAP_ASSET_PATH = "cz.ofpkg"
    }
}
