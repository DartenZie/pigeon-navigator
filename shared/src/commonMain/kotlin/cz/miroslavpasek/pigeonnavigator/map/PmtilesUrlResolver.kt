package cz.miroslavpasek.pigeonnavigator.map

class PmtilesUrlResolver(
    private val tileArchiveFileStore: TileArchiveFileStore = TileArchiveFileStore()
) {

    fun resolve(location: TileArchiveLocation): String = when (location) {
        is TileArchiveLocation.Asset -> "pmtiles://${tileArchiveFileStore.ensureLocalFileUrl(location.assetPath)}"
        is TileArchiveLocation.Remote -> "pmtiles://${location.url}"
        is TileArchiveLocation.LocalFile -> {
            val fileUrl = if (location.absolutePath.startsWith("file://")) {
                location.absolutePath
            } else {
                "file://${location.absolutePath}"
            }
            "pmtiles://$fileUrl"
        }
    }
}
