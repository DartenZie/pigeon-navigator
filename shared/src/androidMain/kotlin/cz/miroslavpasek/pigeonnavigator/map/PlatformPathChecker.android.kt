package cz.miroslavpasek.pigeonnavigator.map

import java.io.File

actual class PlatformPathChecker {
    actual fun exists(absolutePath: String): Boolean = File(absolutePath).exists()
}
