package cz.miroslavpasek.pigeonnavigator.data.aviation.platform

/**
 * Decompresses raw DEFLATE payloads used by ZIP entries.
 */
expect class DeflateDecoder() {
    /**
     * Returns decompressed bytes or null when decompression fails.
     */
    fun decode(rawDeflatedBytes: ByteArray): ByteArray?
}
