package cz.miroslavpasek.pigeonnavigator.data.aviation.platform

/**
 * Resolves bundled assets into absolute local file paths.
 */
expect class PackageAssetResolver() {
    /**
     * Returns an absolute local path for [assetPath], or null when it cannot be resolved.
     */
    fun resolveToLocalPath(assetPath: String): String?
}
