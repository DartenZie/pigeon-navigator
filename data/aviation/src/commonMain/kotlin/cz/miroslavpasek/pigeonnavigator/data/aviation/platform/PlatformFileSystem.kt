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

    /**
     * Returns the size of the file at [path] in bytes, or null if it doesn't exist / on error.
     */
    fun fileSize(path: String): Long?

    /**
     * Reads exactly [length] bytes starting at [fileOffset] from [path] into a new ByteArray.
     * Returns null on any IO error or if the range is out of bounds.
     */
    fun readBytesAt(path: String, fileOffset: Long, length: Int): ByteArray?

    fun writeBytes(path: String, bytes: ByteArray): Boolean

    /** Write a sub-range of [source] starting at [offset] for [length] bytes. */
    fun writeBytesFromArray(path: String, source: ByteArray, offset: Int, length: Int): Boolean

    /**
     * Copy [length] bytes starting at [srcOffset] in [srcPath] to [dstPath].
     * Uses the platform random-access file API — the source bytes never enter the Kotlin heap.
     * Returns false on any IO error.
     */
    fun copyFileRange(srcPath: String, srcOffset: Long, length: Long, dstPath: String): Boolean

    /**
     * Compute SHA-256 over [length] bytes starting at [fileOffset] in [path],
     * reading in chunks so the full range is never loaded into the Kotlin heap at once.
     * Returns the lowercase hex digest, or null on any IO error.
     */
    fun hashFileRange(path: String, fileOffset: Long, length: Long): String?

    fun move(fromPath: String, toPath: String): Boolean

    fun deleteRecursively(path: String): Boolean
}
