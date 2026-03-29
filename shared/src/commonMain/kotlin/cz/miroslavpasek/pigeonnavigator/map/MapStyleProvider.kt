package cz.miroslavpasek.pigeonnavigator.map

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

class MapStyleProvider(
    private val config: MapStyleConfig = MapStyleConfig(),
    private val assetLoader: PlatformAssetLoader = PlatformAssetLoader(),
    private val urlResolver: PmtilesUrlResolver = PmtilesUrlResolver()
) {
    private companion object {
        const val SOURCE_MIN_ZOOM = 5
        const val SOURCE_MAX_ZOOM = 10
        val SOURCE_BOUNDS = listOf(12.08477, 48.54292, 18.86321, 51.06426)
    }

    private var cachedStyleJson: String? = null

    fun getStyleJson(): String {
        cachedStyleJson?.let { return it }

        val baseStyle = assetLoader.readText(config.baseStyleAssetPath)
        val styleJson = injectPmtilesSource(baseStyle)
        cachedStyleJson = styleJson
        return styleJson
    }

    private fun injectPmtilesSource(styleJson: String): String {
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
        rootMap["sources"] = JsonObject(sourcesMap)
        return Json.encodeToString(JsonObject(rootMap))
    }
}
