package cz.miroslavpasek.pigeonnavigator.data.aviation.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH

@OptIn(ExperimentalForeignApi::class)
actual class Sha256Hasher actual constructor() {
    actual fun hashHex(bytes: ByteArray): String = hashHex(bytes, 0, bytes.size)

    actual fun hashHex(bytes: ByteArray, offset: Int, length: Int): String {
        val digest = UByteArray(CC_SHA256_DIGEST_LENGTH)
        bytes.usePinned { inputPinned ->
            digest.usePinned { digestPinned ->
                CC_SHA256(
                    inputPinned.addressOf(offset),
                    length.convert(),
                    digestPinned.addressOf(0)
                )
            }
        }
        return digest.toHex()
    }
}

private fun UByteArray.toHex(): String = joinToString("") { b ->
    b.toInt().toString(16).padStart(2, '0')
}
