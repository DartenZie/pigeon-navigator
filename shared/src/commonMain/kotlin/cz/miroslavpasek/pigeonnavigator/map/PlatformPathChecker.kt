package cz.miroslavpasek.pigeonnavigator.map

/**
 * Resolves whether an absolute local filesystem path exists.
 */
expect class PlatformPathChecker() {
    fun exists(absolutePath: String): Boolean
}
