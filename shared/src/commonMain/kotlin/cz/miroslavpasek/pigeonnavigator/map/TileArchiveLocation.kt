package cz.miroslavpasek.pigeonnavigator.map

sealed interface TileArchiveLocation {
    data class Asset(val assetPath: String) : TileArchiveLocation
    data class Remote(val url: String) : TileArchiveLocation
    data class LocalFile(val absolutePath: String) : TileArchiveLocation
}
