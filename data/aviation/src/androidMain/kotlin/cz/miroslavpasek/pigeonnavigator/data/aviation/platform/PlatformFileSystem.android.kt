package cz.miroslavpasek.pigeonnavigator.data.aviation.platform

import android.content.Context
import java.io.File
import java.io.RandomAccessFile
import java.io.FileOutputStream
import java.security.MessageDigest
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

    actual fun fileSize(path: String): Long? = runCatching {
        val f = File(path)
        if (f.exists()) f.length() else null
    }.getOrNull()

    actual fun readBytesAt(path: String, fileOffset: Long, length: Int): ByteArray? = runCatching {
        RandomAccessFile(path, "r").use { raf ->
            raf.seek(fileOffset)
            val buf = ByteArray(length)
            raf.readFully(buf)
            buf
        }
    }.getOrNull()

    actual fun writeBytes(path: String, bytes: ByteArray): Boolean = runCatching {
        val file = File(path)
        file.parentFile?.mkdirs()
        file.writeBytes(bytes)
        true
    }.getOrElse { false }

    actual fun writeBytesFromArray(path: String, source: ByteArray, offset: Int, length: Int): Boolean = runCatching {
        val file = File(path)
        file.parentFile?.mkdirs()
        file.outputStream().use { it.write(source, offset, length) }
        true
    }.getOrElse { false }

    actual fun copyFileRange(srcPath: String, srcOffset: Long, length: Long, dstPath: String): Boolean = runCatching {
        val dst = File(dstPath)
        dst.parentFile?.mkdirs()
        RandomAccessFile(srcPath, "r").use { src ->
            src.seek(srcOffset)
            FileOutputStream(dst).use { out ->
                val buf = ByteArray(COPY_BUFFER_SIZE)
                var remaining = length
                while (remaining > 0) {
                    val toRead = minOf(buf.size.toLong(), remaining).toInt()
                    val read = src.read(buf, 0, toRead)
                    if (read < 0) return@runCatching false
                    out.write(buf, 0, read)
                    remaining -= read
                }
            }
        }
        true
    }.getOrElse { false }

    actual fun hashFileRange(path: String, fileOffset: Long, length: Long): String? = runCatching {
        val digest = MessageDigest.getInstance("SHA-256")
        RandomAccessFile(path, "r").use { raf ->
            raf.seek(fileOffset)
            val buf = ByteArray(COPY_BUFFER_SIZE)
            var remaining = length
            while (remaining > 0) {
                val toRead = minOf(buf.size.toLong(), remaining).toInt()
                val read = raf.read(buf, 0, toRead)
                if (read < 0) return@runCatching null
                digest.update(buf, 0, read)
                remaining -= read
            }
        }
        digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xFF) }
    }.getOrNull()

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

    private companion object {
        const val COPY_BUFFER_SIZE = 64 * 1024
    }
}
