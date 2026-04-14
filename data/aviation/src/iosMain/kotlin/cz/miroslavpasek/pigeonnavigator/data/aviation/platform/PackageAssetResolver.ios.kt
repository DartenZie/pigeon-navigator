package cz.miroslavpasek.pigeonnavigator.data.aviation.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSBundle
import platform.Foundation.NSFileManager

@OptIn(ExperimentalForeignApi::class)
actual class PackageAssetResolver {
    actual fun resolveToLocalPath(assetPath: String): String? {
        val resourcePath = NSBundle.mainBundle.resourcePath ?: return null
        val candidates = listOf(
            "$resourcePath/MapAssets/$assetPath",
            "$resourcePath/$assetPath"
        )
        return candidates.firstOrNull { NSFileManager.defaultManager.fileExistsAtPath(it) }
    }
}
