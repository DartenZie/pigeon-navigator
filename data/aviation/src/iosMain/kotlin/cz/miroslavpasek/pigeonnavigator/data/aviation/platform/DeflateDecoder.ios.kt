package cz.miroslavpasek.pigeonnavigator.data.aviation.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.usePinned
import platform.posix.memset
import platform.zlib.MAX_WBITS
import platform.zlib.Z_NO_FLUSH
import platform.zlib.Z_OK
import platform.zlib.Z_STREAM_END
import platform.zlib.inflate
import platform.zlib.inflateEnd
import platform.zlib.inflateInit2
import platform.zlib.z_stream_s

@OptIn(ExperimentalForeignApi::class)
actual class DeflateDecoder {
    actual fun decode(rawDeflatedBytes: ByteArray): ByteArray? {
        if (rawDeflatedBytes.isEmpty()) return ByteArray(0)

        return rawDeflatedBytes.usePinned { pinned ->
            memScoped {
                val stream = alloc<z_stream_s>()
                memset(stream.ptr, 0, sizeOf<z_stream_s>().convert())

                stream.next_in = pinned.addressOf(0).reinterpret()
                stream.avail_in = rawDeflatedBytes.size.convert()

                if (inflateInit2(stream.ptr, -MAX_WBITS) != Z_OK) {
                    return@memScoped null
                }

                val chunks = mutableListOf<ByteArray>()
                var status = Z_OK

                while (status == Z_OK) {
                    val chunk = ByteArray(CHUNK_SIZE)
                    status = chunk.usePinned { outPinned ->
                        stream.next_out = outPinned.addressOf(0).reinterpret()
                        stream.avail_out = chunk.size.convert()
                        inflate(stream.ptr, Z_NO_FLUSH)
                    }

                    val produced = CHUNK_SIZE - stream.avail_out.toInt()
                    if (produced > 0) {
                        chunks += chunk.copyOf(produced)
                    }
                }

                inflateEnd(stream.ptr)
                if (status != Z_STREAM_END) {
                    return@memScoped null
                }

                val total = chunks.sumOf { it.size }
                val output = ByteArray(total)
                var offset = 0
                chunks.forEach { chunk ->
                    chunk.copyInto(output, destinationOffset = offset)
                    offset += chunk.size
                }
                output
            }
        }
    }

    private companion object {
        const val CHUNK_SIZE = 16 * 1024
    }
}
