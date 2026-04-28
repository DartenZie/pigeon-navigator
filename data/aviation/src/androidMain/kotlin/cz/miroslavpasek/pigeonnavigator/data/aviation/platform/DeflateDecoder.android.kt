package cz.miroslavpasek.pigeonnavigator.data.aviation.platform

import java.io.ByteArrayOutputStream
import java.util.zip.Inflater

actual class DeflateDecoder {
    actual fun decode(rawDeflatedBytes: ByteArray): ByteArray? {
        val inflater = Inflater(true)
        inflater.setInput(rawDeflatedBytes)
        val output = ByteArrayOutputStream(rawDeflatedBytes.size * 2)
        val buffer = ByteArray(8 * 1024)

        return runCatching {
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                if (count == 0 && inflater.needsInput()) {
                    break
                }
                if (count > 0) {
                    output.write(buffer, 0, count)
                }
            }
            output.toByteArray()
        }.getOrNull().also {
            inflater.end()
        }
    }
}
