package cz.miroslavpasek.pigeonnavigator.map

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSBundle
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.stringWithContentsOfFile

@OptIn(ExperimentalForeignApi::class)
/**
 * iOS bundle-backed implementation of [PlatformAssetLoader].
 */
actual class PlatformAssetLoader {
    /**
     * Reads text content from a bundled resource path.
     */
    actual fun readText(assetPath: String): String {
        val resourcePath = NSBundle.mainBundle.resourcePath
            ?: error("Unable to resolve app bundle resource path")

        val candidates = listOf(
            "$resourcePath/MapAssets/$assetPath",
            "$resourcePath/$assetPath"
        )

        val filePath = candidates.firstOrNull { NSFileManager.defaultManager.fileExistsAtPath(it) }
            ?: error("Asset not found in bundle: $assetPath")

        return NSString.stringWithContentsOfFile(filePath) as? String
            ?: error("Failed to read asset text: $assetPath")
    }
}
