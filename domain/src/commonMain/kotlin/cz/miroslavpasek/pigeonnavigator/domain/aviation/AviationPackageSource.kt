package cz.miroslavpasek.pigeonnavigator.domain.aviation

/**
 * Describes where an OFPKG package file should be read from.
 */
sealed interface AviationPackageSource {
    /**
     * Reads the package from a bundled app asset path.
     */
    data class Asset(val assetPath: String) : AviationPackageSource

    /**
     * Reads the package from an absolute local file path.
     */
    data class LocalFile(val absolutePath: String) : AviationPackageSource
}
