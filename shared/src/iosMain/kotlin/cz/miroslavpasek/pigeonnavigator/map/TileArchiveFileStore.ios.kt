package cz.miroslavpasek.pigeonnavigator.map

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSBundle
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory

@OptIn(ExperimentalForeignApi::class)
/**
 * iOS implementation that copies bundled PMTiles assets to a temporary directory.
 */
actual class TileArchiveFileStore {
    private val fileManager = NSFileManager.defaultManager
    private val tempDir = NSTemporaryDirectory().trimEnd('/')
    private val archivesDir = "$tempDir/pmtiles"

    /**
     * Returns a file URL for [assetPath] copied from app bundle resources.
     */
    actual fun ensureLocalFileUrl(assetPath: String): String {
        val resourcePath = NSBundle.mainBundle.resourcePath
            ?: error("Unable to resolve app bundle resource path")

        val sourcePath = listOf(
            "$resourcePath/MapAssets/$assetPath",
            "$resourcePath/$assetPath"
        ).firstOrNull { fileManager.fileExistsAtPath(it) }
            ?: error("PMTiles asset not found in bundle: $assetPath")

        fileManager.createDirectoryAtPath(
            path = archivesDir,
            withIntermediateDirectories = true,
            attributes = null,
            error = null
        )

        val destinationPath = "$archivesDir/${assetPath.substringAfterLast('/')}"
        if (fileManager.fileExistsAtPath(destinationPath)) {
            fileManager.removeItemAtPath(destinationPath, null)
        }
        fileManager.copyItemAtPath(sourcePath, destinationPath, null)

        return "file://$destinationPath"
    }
}
