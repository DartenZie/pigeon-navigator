package cz.miroslavpasek.pigeonnavigator.map

/**
 * Ensures bundled tile archives are accessible through local file URLs.
 */
expect class TileArchiveFileStore() {
    /**
     * Returns a local file URL for [assetPath], copying the asset when required.
     */
    fun ensureLocalFileUrl(assetPath: String): String
}
