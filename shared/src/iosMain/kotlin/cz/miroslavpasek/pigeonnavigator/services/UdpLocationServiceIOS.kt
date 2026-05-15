package cz.miroslavpasek.pigeonnavigator.services

import cz.miroslavpasek.pigeonnavigator.core.platform.coroutines.DispatcherProvider
import cz.miroslavpasek.pigeonnavigator.data.FlightLocation
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import platform.Foundation.NSLog
import platform.posix.AF_INET
import platform.posix.EINTR
import platform.posix.SOCK_DGRAM
import platform.posix.bind
import platform.posix.close as posixClose
import platform.posix.errno
import platform.posix.recvfrom
import platform.posix.sockaddr
import platform.posix.sockaddr_in
import platform.posix.socket

/**
 * Receives MSFS location packets over UDP and emits [FlightLocation] updates.
 */
@OptIn(ExperimentalForeignApi::class)
class UdpLocationServiceIOS(
    private val listenerConfig: UdpLocationListenerConfig,
    private val parser: UdpLocationParser,
    private val dispatcherProvider: DispatcherProvider,
) : LocationService {

    override fun observeLocationUpdates(): Flow<FlightLocation> = callbackFlow {
        val socketFd = runCatching { openSocket(listenerConfig) }
            .getOrElse { error ->
                NSLog("[UdpLocationServiceIOS] Failed to bind UDP listener: ${error.message ?: "unknown"}")
                close(error)
                return@callbackFlow
            }
        NSLog(
            "[UdpLocationServiceIOS] Listening for UDP location packets on ${listenerConfig.ipAddress}:${listenerConfig.port}"
        )

        val receiveJob: Job = launch(dispatcherProvider.io) {
            val buffer = ByteArray(2048)
            while (isActive) {
                val bytesRead = buffer.usePinned { pinned ->
                    recvfrom(
                        socketFd,
                        pinned.addressOf(0),
                        buffer.size.convert(),
                        0,
                        null,
                        null,
                    )
                }

                when {
                    bytesRead > 0 -> {
                        val location: FlightLocation? = parser.parse(buffer, length = bytesRead.toInt())
                        if (location != null) {
                            NSLog(
                                "[UdpLocationServiceIOS] Received UDP location lat=${location.latitude}, lon=${location.longitude}, alt=${location.altitudeMeters}"
                            )
                            trySend(location)
                        }
                    }

                    bytesRead < 0 -> {
                        if (!isActive) {
                            break
                        }
                        if (errno == EINTR) {
                            continue
                        }
                        NSLog("[UdpLocationServiceIOS] Failed to receive UDP packet, errno=$errno")
                    }
                }
            }
        }

        awaitClose {
            posixClose(socketFd)
            receiveJob.cancel()
        }
    }

    private fun openSocket(config: UdpLocationListenerConfig): Int = memScoped {
        val fd = socket(AF_INET, SOCK_DGRAM, 0)
        if (fd < 0) {
            throw IllegalStateException("Unable to create UDP socket")
        }

        val address = alloc<sockaddr_in>()
        address.sin_family = AF_INET.convert()
        address.sin_port = hostToNetworkPort(config.port)
        applyIpv4Address(address, config.ipAddress)

        val bindResult = bind(
            fd,
            address.ptr.reinterpret<sockaddr>(),
            sizeOf<sockaddr_in>().convert(),
        )
        if (bindResult != 0) {
            posixClose(fd)
            throw IllegalStateException("Unable to bind UDP socket to ${config.ipAddress}:${config.port}")
        }

        fd
    }

    private fun hostToNetworkPort(port: Int): UShort {
        val value = port and 0xFFFF
        return (((value and 0x00FF) shl 8) or ((value and 0xFF00) ushr 8)).toUShort()
    }

    private fun applyIpv4Address(address: sockaddr_in, ipAddress: String) {
        val octets = parseIpv4Address(ipAddress)
        val networkAddress = (
            (octets[0] shl 24) or
                (octets[1] shl 16) or
                (octets[2] shl 8) or
                octets[3]
            ).toUInt()
        address.sin_addr.s_addr = networkAddress
    }

    private fun parseIpv4Address(ipAddress: String): IntArray {
        if (ipAddress == "0.0.0.0") {
            return intArrayOf(0, 0, 0, 0)
        }

        val parts = ipAddress.split('.')
        require(parts.size == 4) { "Invalid IPv4 address: $ipAddress" }

        val octets = IntArray(4)
        for (index in 0..3) {
            val value = parts[index].toIntOrNull()
            require(value != null && value in 0..255) { "Invalid IPv4 address: $ipAddress" }
            octets[index] = value
        }

        return octets
    }
}
