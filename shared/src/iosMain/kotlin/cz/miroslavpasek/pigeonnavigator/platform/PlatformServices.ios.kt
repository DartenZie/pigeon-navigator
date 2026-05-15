package cz.miroslavpasek.pigeonnavigator.platform

import cz.miroslavpasek.pigeonnavigator.core.platform.coroutines.DispatcherProvider
import cz.miroslavpasek.pigeonnavigator.domain.settings.AppSettingsRepository
import cz.miroslavpasek.pigeonnavigator.services.ConfigurableLocationService
import cz.miroslavpasek.pigeonnavigator.services.LocationService
import cz.miroslavpasek.pigeonnavigator.services.LocationServiceConfig
import cz.miroslavpasek.pigeonnavigator.services.LocationServiceIOS
import cz.miroslavpasek.pigeonnavigator.services.MsfsUdpLocationParser
import cz.miroslavpasek.pigeonnavigator.services.UdpLocationServiceIOS
import cz.miroslavpasek.pigeonnavigator.services.XPlaneUdpLocationParser
import org.koin.mp.KoinPlatform
import platform.Foundation.NSLog

/**
 * Creates the iOS [LocationService] backed by CoreLocation.
 */
actual fun createLocationService(): LocationService {
    val koin = KoinPlatform.getKoin()
    val config: LocationServiceConfig = koin.get()
    val settingsRepository: AppSettingsRepository = koin.get()
    val dispatcherProvider: DispatcherProvider = koin.get()
    NSLog(
        "[PlatformServicesIOS] UDP location listener=${config.udpListener.ipAddress}:${config.udpListener.port}"
    )

    val gpsLocationService = LocationServiceIOS()
    val msfsUdpLocationService = UdpLocationServiceIOS(
        listenerConfig = config.udpListener,
        parser = MsfsUdpLocationParser,
        dispatcherProvider = dispatcherProvider,
    )
    val xplaneUdpLocationService = UdpLocationServiceIOS(
        listenerConfig = config.udpListener,
        parser = XPlaneUdpLocationParser(),
        dispatcherProvider = dispatcherProvider,
    )

    return ConfigurableLocationService(
        settingsRepository = settingsRepository,
        gpsLocationService = gpsLocationService,
        msfsUdpLocationService = msfsUdpLocationService,
        xplaneUdpLocationService = xplaneUdpLocationService,
    )
}
