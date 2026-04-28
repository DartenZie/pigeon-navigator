package cz.miroslavpasek.pigeonnavigator.data.aviation.platform

import java.security.MessageDigest

actual class Sha256Hasher actual constructor() {
    actual fun hashHex(bytes: ByteArray): String = hashHex(bytes, 0, bytes.size)

    actual fun hashHex(bytes: ByteArray, offset: Int, length: Int): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(bytes, offset, length)
        return digest.digest().toHex()
    }
}

private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it.toInt() and 0xFF) }
