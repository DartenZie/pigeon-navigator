package cz.miroslavpasek.pigeonnavigator.map

expect class PlatformAssetLoader() {
    fun readText(assetPath: String): String
}
