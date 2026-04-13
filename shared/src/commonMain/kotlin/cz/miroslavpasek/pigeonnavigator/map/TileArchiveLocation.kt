package cz.miroslavpasek.pigeonnavigator.map

/**
 * Describes where a PMTiles archive can be loaded from.
 */
sealed interface TileArchiveLocation {
    /** References an archive bundled as an application asset. */
    data class Asset(val assetPath: String) : TileArchiveLocation

    /** References an archive served from a remote URL. */
    data class Remote(val url: String) : TileArchiveLocation

    /** References an archive stored at an absolute local filesystem path. */
    data class LocalFile(val absolutePath: String) : TileArchiveLocation
}
