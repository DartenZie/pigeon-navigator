package cz.miroslavpasek.pigeonnavigator.services

import cz.miroslavpasek.pigeonnavigator.data.FlightLocation
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.NSError
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
class LocationServiceIOS : LocationService {

    override fun observeLocationUpdates(): Flow<FlightLocation> = callbackFlow {
        val manager = CLLocationManager()

        val sendRequiresPermission = {
            trySend(
                FlightLocation(
                    latitude = 0.0,
                    longitude = 0.0,
                    altitudeMeters = 0.0,
                    speedMetersPerSecond = 0f,
                    bearingDegrees = 0f,
                    requiresPermission = true
                )
            )
        }

        val initialStatus = CLLocationManager.authorizationStatus()
        if (!isAuthorized(initialStatus)) {
            manager.requestWhenInUseAuthorization()
            sendRequiresPermission()
        }

        manager.desiredAccuracy = kCLLocationAccuracyBest
        manager.distanceFilter = 5.0 // 5 meters

        val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
            override fun locationManager(
                manager: CLLocationManager,
                didChangeAuthorizationStatus: CLAuthorizationStatus
            ) {
                if (!isAuthorized(didChangeAuthorizationStatus)) {
                    sendRequiresPermission()
                } else {
                    manager.startUpdatingLocation()
                }
            }

            override fun locationManager(
                manager: CLLocationManager,
                didUpdateLocations: List<*>
            ) {
                val last = didUpdateLocations.lastOrNull() as? CLLocation ?: return

                val speed = last.speed.takeIf { it >= 0.0 }?.toFloat() ?: 0f
                val bearing = last.course.takeIf { it >= 0.0 }?.toFloat() ?: 0f

                trySend(
                    FlightLocation(
                        latitude = last.coordinate.useContents { this.latitude },
                        longitude = last.coordinate.useContents { this.longitude },
                        altitudeMeters = last.altitude,
                        speedMetersPerSecond = speed,
                        bearingDegrees = bearing,
                        requiresPermission = false
                    )
                )
            }

            override fun locationManager(
                manager: CLLocationManager,
                didFailWithError: NSError
            ) {
                // Optional: you could send requiresPermission=false + zeros, or close(error)
                // close(Throwable(didFailWithError.localizedDescription))
            }
        }

        manager.delegate = delegate

        if (isAuthorized(CLLocationManager.authorizationStatus())) {
            manager.startUpdatingLocation()
        }

        awaitClose {
            manager.stopUpdatingLocation()
            manager.delegate = null
        }
    }

    private fun isAuthorized(status: CLAuthorizationStatus): Boolean =
        status == kCLAuthorizationStatusAuthorizedAlways ||
        status == kCLAuthorizationStatusAuthorizedWhenInUse
}