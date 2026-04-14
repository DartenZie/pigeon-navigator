package cz.miroslavpasek.pigeonnavigator.data.aviation.platform

import android.content.Context
import java.io.File
import org.koin.mp.KoinPlatform

actual class PlatformFileSystem {
    private val context: Context = KoinPlatform.getKoin().get()

    actual val filesDirectoryPath: String = context.filesDir.absolutePath

    actual fun exists(path: String): Boolean = File(path).exists()

    actual fun createDirectories(path: String): Boolean {
        val file = File(path)
        return file.exists() || file.mkdirs()
    }

    actual fun readBytes(path: String): ByteArray? = runCatching {
        File(path).readBytes()
    }.getOrNull()

    actual fun writeBytes(path: String, bytes: ByteArray): Boolean = runCatching {
        val file = File(path)
        file.parentFile?.mkdirs()
        file.writeBytes(bytes)
        true
    }.getOrElse { false }

    actual fun move(fromPath: String, toPath: String): Boolean {
        val from = File(fromPath)
        val to = File(toPath)
        to.parentFile?.mkdirs()
        return from.renameTo(to)
    }

    actual fun deleteRecursively(path: String): Boolean {
        val file = File(path)
        return !file.exists() || file.deleteRecursively()
    }
}
