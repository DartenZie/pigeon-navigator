package cz.miroslavpasek.pigeonnavigator.platform

import android.content.Context
import android.util.Log
import com.google.android.gms.location.LocationServices
import cz.miroslavpasek.pigeonnavigator.core.platform.coroutines.DispatcherProvider
import cz.miroslavpasek.pigeonnavigator.domain.settings.AppSettingsRepository
import cz.miroslavpasek.pigeonnavigator.services.ConfigurableLocationService
import cz.miroslavpasek.pigeonnavigator.services.LocationService
import cz.miroslavpasek.pigeonnavigator.services.LocationServiceAndroid
import cz.miroslavpasek.pigeonnavigator.services.LocationServiceConfig
import cz.miroslavpasek.pigeonnavigator.services.MsfsUdpLocationParser
import cz.miroslavpasek.pigeonnavigator.services.UdpLocationServiceAndroid
import cz.miroslavpasek.pigeonnavigator.services.XPlaneUdpLocationParser
import org.koin.mp.KoinPlatform

/**
 * Creates the Android [LocationService] backed by fused location provider APIs.
 */
actual fun createLocationService(): LocationService {
    val koin = KoinPlatform.getKoin()
    val context: Context = koin.get()
    val config: LocationServiceConfig = koin.get()
    val settingsRepository: AppSettingsRepository = koin.get()
    val dispatcherProvider: DispatcherProvider = koin.get()
    Log.i(
        "PlatformServicesAndroid",
        "UDP location listener=${config.udpListener.ipAddress}:${config.udpListener.port}"
    )

    val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    val gpsLocationService = LocationServiceAndroid(
        context = context,
        fusedClient = fusedClient
    )

    val msfsUdpLocationService = UdpLocationServiceAndroid(
        context = context,
        listenerConfig = config.udpListener,
        parser = MsfsUdpLocationParser,
        dispatcherProvider = dispatcherProvider,
    )
    val xplaneUdpLocationService = UdpLocationServiceAndroid(
        context = context,
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
