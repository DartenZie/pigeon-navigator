package cz.miroslavpasek.pigeonnavigator.map

data class MapStyleConfig(
    val baseStyleAssetPath: String = "style.json",
    val pmtilesSourceId: String = "pmtiles-source",
    val tileArchiveLocation: TileArchiveLocation = TileArchiveLocation.Asset("cz.pmtiles"),
    val terrainDemSourceId: String = "terrain-dem-source",
    val terrainArchiveLocation: TileArchiveLocation = TileArchiveLocation.Asset("terrain.pmtiles"),
    val hillshadeLayerId: String = "terrain-hillshade"
) {
    constructor(
        baseStyleAssetPath: String,
        pmtilesSourceId: String,
        tileArchiveLocation: TileArchiveLocation
    ) : this(
        baseStyleAssetPath = baseStyleAssetPath,
        pmtilesSourceId = pmtilesSourceId,
        tileArchiveLocation = tileArchiveLocation,
        terrainDemSourceId = "terrain-dem-source",
        terrainArchiveLocation = TileArchiveLocation.Asset("terrain.pmtiles"),
        hillshadeLayerId = "terrain-hillshade"
    )
}
