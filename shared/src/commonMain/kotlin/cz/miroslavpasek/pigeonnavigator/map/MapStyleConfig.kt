package cz.miroslavpasek.pigeonnavigator.map

/**
 * Defines style asset and source identifiers used to build runtime map style JSON.
 *
 * Note: [tileArchiveLocation] and [terrainArchiveLocation] defaults are never used in
 * production — [MapStyleProvider] returns null when no active package with valid on-disk
 * files is present, so these fields are only meaningful when explicitly overridden via
 * [copy] inside the provider.
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
    val tileArchiveLocation: TileArchiveLocation = TileArchiveLocation.LocalFile(""),
    val terrainDemSourceId: String = "terrain-dem-source",
    val terrainArchiveLocation: TileArchiveLocation = TileArchiveLocation.LocalFile(""),
    val hillshadeLayerId: String = "terrain-hillshade"
)
