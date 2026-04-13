package cz.miroslavpasek.pigeonnavigator

/**
 * Builds a greeting that includes the current platform name.
 */
class Greeting {
    private val platform = getPlatform()

    /**
     * Returns a human-readable greeting string.
     */
    fun greet(): String {
        return "Hello, ${platform.name}!"
    }
}
