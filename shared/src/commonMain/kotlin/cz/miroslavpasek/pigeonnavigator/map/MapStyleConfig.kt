package cz.miroslavpasek.pigeonnavigator.map

/**
 * Defines style asset and source identifiers used to build runtime map style JSON.
 *
 * @property baseStyleAssetPath Asset path to the base style JSON template.
 * @property pmtilesSourceId Source id used for vector PMTiles layers.
 * @property tileArchiveLocation Location of the vector PMTiles archive.
 * @property terrainDemSourceId Source id used for terrain DEM tiles.
 * @property terrainArchiveLocation Location of the terrain PMTiles archive.
 * @property hillshadeLayerId Layer id used for generated hillshade layer.
 */
data class MapStyleConfig(
    val baseStyleAssetPath: String = "style.json",
    val pmtilesSourceId: String = "pmtiles-source",
    val tileArchiveLocation: TileArchiveLocation = TileArchiveLocation.Asset("cz.pmtiles"),
    val terrainDemSourceId: String = "terrain-dem-source",
    val terrainArchiveLocation: TileArchiveLocation = TileArchiveLocation.Asset("terrain.pmtiles"),
    val hillshadeLayerId: String = "terrain-hillshade"
) {
    /**
     * Creates config with default terrain settings while overriding base style and vector source.
     */
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
