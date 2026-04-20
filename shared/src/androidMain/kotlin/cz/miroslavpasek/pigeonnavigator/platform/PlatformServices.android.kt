package cz.miroslavpasek.pigeonnavigator.platform

import android.content.Context
import android.util.Log
import com.google.android.gms.location.LocationServices
import cz.miroslavpasek.pigeonnavigator.core.platform.coroutines.DispatcherProvider
import cz.miroslavpasek.pigeonnavigator.services.ConfigurableLocationService
import cz.miroslavpasek.pigeonnavigator.services.LocationService
import cz.miroslavpasek.pigeonnavigator.services.LocationServiceAndroid
import cz.miroslavpasek.pigeonnavigator.services.LocationServiceConfig
import cz.miroslavpasek.pigeonnavigator.services.UdpLocationServiceAndroid
import org.koin.mp.KoinPlatform

/**
 * Creates the Android [LocationService] backed by fused location provider APIs.
 */
actual fun createLocationService(): LocationService {
    val koin = KoinPlatform.getKoin()
    val context: Context = koin.get()
    val config: LocationServiceConfig = koin.get()
    val dispatcherProvider: DispatcherProvider = koin.get()
    Log.i(
        "PlatformServicesAndroid",
        "Location source=${config.source}, udp=${config.udpListener.ipAddress}:${config.udpListener.port}"
    )

    val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    val gpsLocationService = LocationServiceAndroid(
        context = context,
        fusedClient = fusedClient
    )

    val udpLocationService = UdpLocationServiceAndroid(
        listenerConfig = config.udpListener,
        dispatcherProvider = dispatcherProvider,
    )

    return ConfigurableLocationService(
        config = config,
        gpsLocationService = gpsLocationService,
        udpLocationService = udpLocationService,
    )
}
