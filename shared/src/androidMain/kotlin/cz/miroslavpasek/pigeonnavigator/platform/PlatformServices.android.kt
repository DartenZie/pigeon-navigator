package cz.miroslavpasek.pigeonnavigator.platform

import android.content.Context
import com.google.android.gms.location.LocationServices
import cz.miroslavpasek.pigeonnavigator.services.LocationService
import cz.miroslavpasek.pigeonnavigator.services.LocationServiceAndroid
import org.koin.mp.KoinPlatform

/**
 * Creates the Android [LocationService] backed by fused location provider APIs.
 */
actual fun createLocationService(): LocationService {
    val koin = KoinPlatform.getKoin()
    val context: Context = koin.get()

    val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    return LocationServiceAndroid(
        context = context,
        fusedClient = fusedClient
    )
}
