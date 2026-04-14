package cz.miroslavpasek.pigeonnavigator.data.aviation.platform

/**
 * Small platform file system facade used by package installation flow.
 */
expect class PlatformFileSystem() {
    /** Absolute app-private files directory path. */
    val filesDirectoryPath: String

    fun exists(path: String): Boolean

    fun createDirectories(path: String): Boolean

    fun readBytes(path: String): ByteArray?

    fun writeBytes(path: String, bytes: ByteArray): Boolean

    fun move(fromPath: String, toPath: String): Boolean

    fun deleteRecursively(path: String): Boolean
}
