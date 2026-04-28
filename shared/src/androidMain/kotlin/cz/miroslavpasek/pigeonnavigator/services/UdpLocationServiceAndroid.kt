package cz.miroslavpasek.pigeonnavigator.services

import android.util.Log
import cz.miroslavpasek.pigeonnavigator.core.platform.coroutines.DispatcherProvider
import cz.miroslavpasek.pigeonnavigator.data.FlightLocation
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.SocketException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Receives MSFS location packets over UDP and emits [FlightLocation] updates.
 */
class UdpLocationServiceAndroid(
    private val listenerConfig: UdpLocationListenerConfig,
    private val dispatcherProvider: DispatcherProvider,
) : LocationService {

    override fun observeLocationUpdates(): Flow<FlightLocation> = callbackFlow {
        val socket = try {
            DatagramSocket(null).apply {
                reuseAddress = true
                bind(InetSocketAddress(listenerConfig.ipAddress, listenerConfig.port))
            }
        } catch (error: Exception) {
            Log.e("UdpLocationService", "Failed to bind UDP listener", error)
            close(error)
            return@callbackFlow
        }
        Log.i(
            "UdpLocationService",
            "Listening for UDP location packets on ${listenerConfig.ipAddress}:${listenerConfig.port}"
        )

        val receiveJob: Job = launch(dispatcherProvider.io) {
            val buffer = ByteArray(2048)

            while (isActive) {
                val packet = DatagramPacket(buffer, buffer.size)
                try {
                    socket.receive(packet)
                    val payload = String(packet.data, packet.offset, packet.length, Charsets.UTF_8)
                    val location = parseMsfsUdpLocationPacket(payload)
                    if (location != null) {
                        Log.d(
                            "UdpLocationService",
                            "Received UDP location lat=${location.latitude}, lon=${location.longitude}, alt=${location.altitudeMeters}"
                        )
                        trySend(location)
                    }
                } catch (_: SocketException) {
                    if (!isActive) {
                        break
                    }
                } catch (error: Exception) {
                    Log.w("UdpLocationService", "Failed to process UDP packet", error)
                }
            }
        }

        awaitClose {
            receiveJob.cancel()
            socket.close()
        }
    }
}
