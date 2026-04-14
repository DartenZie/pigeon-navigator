package cz.miroslavpasek.pigeonnavigator.data.aviation.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.Foundation.NSFileManager
import platform.Foundation.NSHomeDirectory
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.memcpy
import platform.posix.fwrite

@OptIn(ExperimentalForeignApi::class)
actual class PlatformFileSystem {
    private val fileManager = NSFileManager.defaultManager

    actual val filesDirectoryPath: String = "${NSHomeDirectory()}/Documents"

    actual fun exists(path: String): Boolean = fileManager.fileExistsAtPath(path)

    actual fun createDirectories(path: String): Boolean {
        fileManager.createDirectoryAtPath(
            path = path,
            withIntermediateDirectories = true,
            attributes = null,
            error = null
        )
        return true
    }

    actual fun readBytes(path: String): ByteArray? {
        val data = fileManager.contentsAtPath(path) ?: return null
        val size = data.length.toInt()
        if (size <= 0) return ByteArray(0)

        val bytes = ByteArray(size)
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), data.bytes, size.convert())
        }
        return bytes
    }

    actual fun writeBytes(path: String, bytes: ByteArray): Boolean {
        val parentPath = path.substringBeforeLast('/', missingDelimiterValue = "")
        if (parentPath.isNotEmpty()) {
            createDirectories(parentPath)
        }

        val file = fopen(path, "wb") ?: return false
        val written = bytes.usePinned { pinned ->
            fwrite(pinned.addressOf(0), 1.convert(), bytes.size.convert(), file)
        }
        fclose(file)
        return written.toInt() == bytes.size
    }

    actual fun move(fromPath: String, toPath: String): Boolean {
        val parentPath = toPath.substringBeforeLast('/', missingDelimiterValue = "")
        if (parentPath.isNotEmpty()) {
            createDirectories(parentPath)
        }
        fileManager.removeItemAtPath(toPath, null)
        return fileManager.moveItemAtPath(fromPath, toPath, null)
    }

    actual fun deleteRecursively(path: String): Boolean {
        if (!fileManager.fileExistsAtPath(path)) return true
        return fileManager.removeItemAtPath(path, null)
    }
}
