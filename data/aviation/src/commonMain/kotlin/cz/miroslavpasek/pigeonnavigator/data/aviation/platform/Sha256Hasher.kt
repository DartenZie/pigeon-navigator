package cz.miroslavpasek.pigeonnavigator.data.aviation.platform

/**
 * Computes SHA-256 hashes for package integrity checks.
 * Each platform delegates to its native crypto implementation.
 */
expect class Sha256Hasher() {
    fun hashHex(bytes: ByteArray): String
    fun hashHex(bytes: ByteArray, offset: Int, length: Int): String
}
