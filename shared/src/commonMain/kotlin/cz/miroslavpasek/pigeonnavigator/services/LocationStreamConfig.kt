package cz.miroslavpasek.pigeonnavigator.services

/**
 * Selects which source should drive location updates.
 */
enum class LocationStreamSource {
    DeviceGps,
    UdpDebug,
}

/**
 * UDP listener endpoint used to receive simulated GPS packets.
 */
data class UdpLocationListenerConfig(
    val ipAddress: String,
    val port: Int,
) {
    init {
        require(port in 1..65535) { "UDP port must be in 1..65535" }
    }
}

/**
 * Static configuration for the location service source selection.
 */
data class LocationServiceConfig(
    val source: LocationStreamSource = LocationStreamSource.DeviceGps,
    val udpListener: UdpLocationListenerConfig = UdpLocationListenerConfig(
        ipAddress = "0.0.0.0",
        port = 49002,
    ),
)
