package cz.miroslavpasek.pigeonnavigator

/**
 * Describes the current runtime platform.
 */
interface Platform {
    val name: String
}

/**
 * Returns information about the current runtime platform.
 */
expect fun getPlatform(): Platform
