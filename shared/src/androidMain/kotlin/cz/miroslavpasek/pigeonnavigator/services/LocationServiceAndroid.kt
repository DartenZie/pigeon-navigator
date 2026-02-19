package cz.miroslavpasek.pigeonnavigator.services

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import cz.miroslavpasek.pigeonnavigator.data.FlightLocation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class LocationServiceAndroid(
    private val context: Context,
    private val fusedClient: FusedLocationProviderClient
) : LocationService {

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @Suppress("MissingPermission")
    override fun observeLocationUpdates(): Flow<FlightLocation> = callbackFlow {
        val locationCallback: LocationCallback? =
            if (!hasLocationPermission()) {
                trySend(
                    FlightLocation(
                        latitude = 0.0,
                        longitude = 0.0,
                        altitudeMeters = 0.0,
                        speedMetersPerSecond = 0f,
                        bearingDegrees = 0f,
                        requiresPermission = true,
                    )
                )
                null
            } else {
                val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 500L)
                    .setMinUpdateDistanceMeters(5f)
                    .build()

                val callback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        super.onLocationResult(result)

                        val location: Location? = result.lastLocation
                        if (location != null) {
                            trySend(
                                FlightLocation(
                                    latitude = location.latitude,
                                    longitude = location.longitude,
                                    altitudeMeters = location.altitude,
                                    speedMetersPerSecond = location.speed,
                                    bearingDegrees = location.bearing,
                                    requiresPermission = false,
                                )
                            )
                        }
                    }
                }

                fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
                callback
            }

        awaitClose {
            locationCallback?.let { fusedClient.removeLocationUpdates(it) }
        }
    }
}