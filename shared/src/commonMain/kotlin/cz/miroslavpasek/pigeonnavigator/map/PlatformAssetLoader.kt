package cz.miroslavpasek.pigeonnavigator.map

/**
 * Loads text assets from a platform-specific application bundle.
 */
expect class PlatformAssetLoader() {
    /**
     * Returns full text contents for [assetPath].
     */
    fun readText(assetPath: String): String
}
