package cz.miroslavpasek.pigeonnavigator.services

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
 * Static UDP listener configuration. Source selection lives in app settings.
 */
data class LocationServiceConfig(
    val udpListener: UdpLocationListenerConfig = UdpLocationListenerConfig(
        ipAddress = "0.0.0.0",
        port = 49002,
    ),
)
