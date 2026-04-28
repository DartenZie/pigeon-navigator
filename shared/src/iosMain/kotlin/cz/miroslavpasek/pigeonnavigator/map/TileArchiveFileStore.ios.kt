package cz.miroslavpasek.pigeonnavigator.map

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSBundle
import platform.Foundation.NSFileManager

@OptIn(ExperimentalForeignApi::class)
/**
 * iOS implementation that resolves bundled PMTiles assets to bundle file URLs.
 */
actual class TileArchiveFileStore {
    private val fileManager = NSFileManager.defaultManager

    /**
     * Returns a file URL for [assetPath] from app bundle resources.
     */
    actual fun ensureLocalFileUrl(assetPath: String): String {
        val resourcePath = NSBundle.mainBundle.resourcePath
            ?: error("Unable to resolve app bundle resource path")

        val sourcePath = listOf(
            "$resourcePath/MapAssets/$assetPath",
            "$resourcePath/$assetPath"
        ).firstOrNull { fileManager.fileExistsAtPath(it) }
            ?: error("PMTiles asset not found in bundle: $assetPath")

        return "file://$sourcePath"
    }
}
