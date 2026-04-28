package cz.miroslavpasek.pigeonnavigator.map

import platform.Foundation.NSFileManager

actual class PlatformPathChecker {
    private val fileManager = NSFileManager.defaultManager

    actual fun exists(absolutePath: String): Boolean = fileManager.fileExistsAtPath(absolutePath)
}
