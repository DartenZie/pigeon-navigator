package cz.miroslavpasek.pigeonnavigator.data.aviation.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_SHA256_CTX
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.CoreCrypto.CC_SHA256_Final
import platform.CoreCrypto.CC_SHA256_Init
import platform.CoreCrypto.CC_SHA256_Update
import platform.Foundation.NSFileManager
import platform.Foundation.NSHomeDirectory
import platform.posix.SEEK_SET
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread
import platform.posix.fseek
import platform.posix.fwrite
import platform.posix.memcpy
import platform.posix.stat

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

    actual fun fileSize(path: String): Long? = runCatching {
        val st = nativeHeap.alloc<stat>()
        val result = platform.posix.stat(path, st.ptr)
        val size = if (result == 0) st.st_size else null
        nativeHeap.free(st.rawPtr)
        size
    }.getOrNull()

    actual fun readBytesAt(path: String, fileOffset: Long, length: Int): ByteArray? = runCatching {
        val file = fopen(path, "rb") ?: return null
        try {
            if (fseek(file, fileOffset, SEEK_SET) != 0) return null
            val buf = ByteArray(length)
            val read = buf.usePinned { pinned ->
                fread(pinned.addressOf(0), 1.convert(), length.convert(), file).toInt()
            }
            if (read != length) null else buf
        } finally {
            fclose(file)
        }
    }.getOrNull()

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

    actual fun writeBytesFromArray(path: String, source: ByteArray, offset: Int, length: Int): Boolean {
        val parentPath = path.substringBeforeLast('/', missingDelimiterValue = "")
        if (parentPath.isNotEmpty()) {
            createDirectories(parentPath)
        }

        val file = fopen(path, "wb") ?: return false
        val written = source.usePinned { pinned ->
            fwrite(pinned.addressOf(offset), 1.convert(), length.convert(), file)
        }
        fclose(file)
        return written.toInt() == length
    }

    actual fun copyFileRange(srcPath: String, srcOffset: Long, length: Long, dstPath: String): Boolean {
        val parentPath = dstPath.substringBeforeLast('/', missingDelimiterValue = "")
        if (parentPath.isNotEmpty()) {
            createDirectories(parentPath)
        }

        val src = fopen(srcPath, "rb") ?: return false
        val dst = fopen(dstPath, "wb")
        if (dst == null) {
            fclose(src)
            return false
        }

        return runCatching {
            if (fseek(src, srcOffset, SEEK_SET) != 0) return@runCatching false
            val buf = ByteArray(COPY_BUFFER_SIZE)
            var remaining = length
            while (remaining > 0) {
                val toRead = minOf(buf.size.toLong(), remaining).toInt()
                val read = buf.usePinned { pinned ->
                    fread(pinned.addressOf(0), 1.convert(), toRead.convert(), src).toInt()
                }
                if (read <= 0) return@runCatching false
                val written = buf.usePinned { pinned ->
                    fwrite(pinned.addressOf(0), 1.convert(), read.convert(), dst).toInt()
                }
                if (written != read) return@runCatching false
                remaining -= read
            }
            true
        }.getOrElse { false }.also {
            fclose(src)
            fclose(dst)
        }
    }

    /**
     * Incremental SHA-256 over [length] bytes at [fileOffset] in [path].
     * Uses CC_SHA256_Init/Update/Final via POSIX fread in 64 KB chunks —
     * the entire range never enters the Kotlin heap at once.
     */
    actual fun hashFileRange(path: String, fileOffset: Long, length: Long): String? {
        val file = fopen(path, "rb") ?: return null
        val ctx = nativeHeap.alloc<CC_SHA256_CTX>()
        return runCatching {
            if (fseek(file, fileOffset, SEEK_SET) != 0) return@runCatching null
            CC_SHA256_Init(ctx.ptr)
            val buf = ByteArray(COPY_BUFFER_SIZE)
            var remaining = length
            while (remaining > 0) {
                val toRead = minOf(buf.size.toLong(), remaining).toInt()
                val read = buf.usePinned { pinned ->
                    fread(pinned.addressOf(0), 1.convert(), toRead.convert(), file).toInt()
                }
                if (read <= 0) return@runCatching null
                buf.usePinned { pinned ->
                    CC_SHA256_Update(ctx.ptr, pinned.addressOf(0), read.convert())
                }
                remaining -= read
            }
            val digest = UByteArray(CC_SHA256_DIGEST_LENGTH)
            digest.usePinned { pinned ->
                CC_SHA256_Final(pinned.addressOf(0), ctx.ptr)
            }
            digest.joinToString("") { b -> b.toInt().toString(16).padStart(2, '0') }
        }.getOrNull().also {
            nativeHeap.free(ctx.rawPtr)
            fclose(file)
        }
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

    private companion object {
        const val COPY_BUFFER_SIZE = 64 * 1024
    }
}
