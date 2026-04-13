package cz.miroslavpasek.pigeonnavigator.map

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

/**
 * Builds map style JSON by injecting PMTiles sources and a hillshade layer into a base style.
 *
 * The computed style is cached after first build.
 */
class MapStyleProvider(
    private val config: MapStyleConfig = MapStyleConfig(),
    private val assetLoader: PlatformAssetLoader = PlatformAssetLoader(),
    private val urlResolver: PmtilesUrlResolver = PmtilesUrlResolver()
) {
    private companion object {
        const val SOURCE_MIN_ZOOM = 5
        const val SOURCE_MAX_ZOOM = 10
        val SOURCE_BOUNDS = listOf(12.08477, 48.54292, 18.86321, 51.06426)

        const val TERRAIN_SOURCE_MIN_ZOOM = 6
        const val TERRAIN_SOURCE_MAX_ZOOM = 12
        const val TERRAIN_TILE_SIZE = 256
        const val HILLSHADE_EXAGGERATION = 0.35
    }

    private var cachedStyleJson: String? = null

    /**
     * Returns runtime style JSON with terrain source and hillshade injected.
     */
    fun getStyleJson(): String {
        cachedStyleJson?.let { return it }

        val baseStyle = assetLoader.readText(config.baseStyleAssetPath)
        val styleJson = injectSourcesAndHillshade(baseStyle)
        cachedStyleJson = styleJson
        return styleJson
    }

    private fun injectSourcesAndHillshade(styleJson: String): String {
        val rootMap = Json.parseToJsonElement(styleJson).jsonObject.toMutableMap()
        val sourcesMap = (rootMap["sources"] as? JsonObject)?.toMutableMap() ?: mutableMapOf()

        sourcesMap[config.pmtilesSourceId] = buildJsonObject {
            put("type", "vector")
            put("url", urlResolver.resolve(config.tileArchiveLocation))
            put("minzoom", SOURCE_MIN_ZOOM)
            put("maxzoom", SOURCE_MAX_ZOOM)
            put("bounds", buildJsonArray {
                SOURCE_BOUNDS.forEach { add(JsonPrimitive(it)) }
            })
        }

        sourcesMap[config.terrainDemSourceId] = buildJsonObject {
            put("type", "raster-dem")
            put("url", urlResolver.resolve(config.terrainArchiveLocation))
            put("encoding", "terrarium")
            put("tileSize", TERRAIN_TILE_SIZE)
            put("minzoom", TERRAIN_SOURCE_MIN_ZOOM)
            put("maxzoom", TERRAIN_SOURCE_MAX_ZOOM)
            put("bounds", buildJsonArray {
                SOURCE_BOUNDS.forEach { add(JsonPrimitive(it)) }
            })
        }

        rootMap["sources"] = JsonObject(sourcesMap)

        val layers = (rootMap["layers"] as? JsonArray)?.toMutableList() ?: mutableListOf()
        if (layers.none { it.layerId == config.hillshadeLayerId }) {
            val hillshadeLayer = buildJsonObject {
                put("id", config.hillshadeLayerId)
                put("type", "hillshade")
                put("source", config.terrainDemSourceId)
                put("layout", buildJsonObject {
                    put("visibility", "visible")
                })
                put("paint", buildJsonObject {
                    put("hillshade-exaggeration", HILLSHADE_EXAGGERATION)
                })
            }

            val backgroundIndex = layers.indexOfFirst { it.layerId == "background" }
            if (backgroundIndex >= 0) {
                layers.add(backgroundIndex + 1, hillshadeLayer)
            } else {
                layers.add(0, hillshadeLayer)
            }
        }

        rootMap["layers"] = JsonArray(layers)

        return Json.encodeToString(JsonObject(rootMap))
    }

    private val JsonElement.layerId: String?
        get() = (this as? JsonObject)?.get("id")?.let { (it as? JsonPrimitive)?.content }
}
