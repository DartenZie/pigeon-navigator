package cz.miroslavpasek.pigeonnavigator.map

/**
 * Swift-friendly bridge for resolving the runtime map style JSON.
 */
class MapStyleBridge {
    fun resolveStyleJson(): String? = MapStyleProvider().getStyleJson()
}
