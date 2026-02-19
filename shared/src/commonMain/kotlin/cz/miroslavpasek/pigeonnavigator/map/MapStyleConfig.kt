package cz.miroslavpasek.pigeonnavigator.map

data class MapStyleConfig(
    val baseStyleAssetPath: String = "style.json",
    val pmtilesSourceId: String = "pmtiles-source",
    val tileArchiveLocation: TileArchiveLocation = TileArchiveLocation.Asset("cz.pmtiles")
)
