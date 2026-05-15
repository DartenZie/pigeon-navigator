package cz.miroslavpasek.pigeonnavigator.services

import android.content.Context
import android.util.Log
import android.net.wifi.WifiManager
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
    private val context: Context,
    private val listenerConfig: UdpLocationListenerConfig,
    private val parser: UdpLocationParser,
    private val dispatcherProvider: DispatcherProvider,
) : LocationService {

    override fun observeLocationUpdates(): Flow<FlightLocation> = callbackFlow {
        val multicastLock = acquireMulticastLock()
        val socket = try {
            DatagramSocket(null).apply {
                reuseAddress = true
                broadcast = true
                bind(InetSocketAddress(listenerConfig.ipAddress, listenerConfig.port))
            }
        } catch (error: Exception) {
            multicastLock?.release()
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
                    val location = parser.parse(packet.data, packet.offset, packet.length)
                    if (location != null) {
                        Log.d(
                            "UdpLocationService",
                            "Received UDP location lat=${location.latitude}, lon=${location.longitude}, alt=${location.altitudeMeters}"
                        )
                        trySend(location)
                    } else {
                        Log.i("UdpLocationService", "Ignored UDP packet length=${packet.length}")
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
            multicastLock?.release()
        }
    }

    private fun acquireMulticastLock(): WifiManager.MulticastLock? {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        if (wifiManager == null) {
            Log.w("UdpLocationService", "WifiManager unavailable; UDP broadcast packets may be filtered")
            return null
        }

        return wifiManager.createMulticastLock("PigeonNavigatorUdpLocation").apply {
            setReferenceCounted(false)
            acquire()
            Log.i("UdpLocationService", "Acquired Wi-Fi multicast lock for UDP location packets")
        }
    }
}
