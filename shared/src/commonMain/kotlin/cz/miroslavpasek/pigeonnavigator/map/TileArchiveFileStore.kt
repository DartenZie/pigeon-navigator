package cz.miroslavpasek.pigeonnavigator.map

expect class TileArchiveFileStore() {
    fun ensureLocalFileUrl(assetPath: String): String
}
