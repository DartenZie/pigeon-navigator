package cz.miroslavpasek.pigeonnavigator.platform

import cz.miroslavpasek.pigeonnavigator.core.platform.coroutines.DispatcherProvider
import cz.miroslavpasek.pigeonnavigator.services.ConfigurableLocationService
import cz.miroslavpasek.pigeonnavigator.services.LocationService
import cz.miroslavpasek.pigeonnavigator.services.LocationServiceConfig
import cz.miroslavpasek.pigeonnavigator.services.LocationServiceIOS
import cz.miroslavpasek.pigeonnavigator.services.UdpLocationServiceIOS
import org.koin.mp.KoinPlatform
import platform.Foundation.NSLog

/**
 * Creates the iOS [LocationService] backed by CoreLocation.
 */
actual fun createLocationService(): LocationService {
    val koin = KoinPlatform.getKoin()
    val config: LocationServiceConfig = koin.get()
    val dispatcherProvider: DispatcherProvider = koin.get()
    NSLog(
        "[PlatformServicesIOS] Location source=${config.source}, udp=${config.udpListener.ipAddress}:${config.udpListener.port}"
    )

    val gpsLocationService = LocationServiceIOS()
    val udpLocationService = UdpLocationServiceIOS(
        listenerConfig = config.udpListener,
        dispatcherProvider = dispatcherProvider,
    )

    return ConfigurableLocationService(
        config = config,
        gpsLocationService = gpsLocationService,
        udpLocationService = udpLocationService,
    )
}
