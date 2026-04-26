package cz.miroslavpasek.pigeonnavigator.services

import cz.miroslavpasek.pigeonnavigator.data.FlightLocation
import cz.miroslavpasek.pigeonnavigator.data.LocationStatus
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
/**
 * Streams iOS CoreLocation updates as [FlightLocation] values.
 */
class LocationServiceIOS : LocationService {

    /**
     * Returns a callback-backed flow of location updates.
     *
     * Emits a permission-required marker update until authorization is granted.
     */
    override fun observeLocationUpdates(): Flow<FlightLocation> = callbackFlow {
        val manager = CLLocationManager()
        var retainedDelegate: NSObject? = null

        val sendRequiresPermission = {
            trySend(
                FlightLocation(
                    latitude = 0.0,
                    longitude = 0.0,
                    altitudeMeters = 0.0,
                    speedMetersPerSecond = 0f,
                    bearingDegrees = 0f,
                    horizontalAccuracyMeters = null,
                    requiresPermission = true,
                    status = LocationStatus.PermissionRequired
                )
            )
        }

        val sendSignalLost = {
            trySend(
                FlightLocation(
                    latitude = 0.0,
                    longitude = 0.0,
                    altitudeMeters = 0.0,
                    speedMetersPerSecond = 0f,
                    bearingDegrees = 0f,
                    horizontalAccuracyMeters = null,
                    requiresPermission = false,
                    status = LocationStatus.SignalLost
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

            override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
                if (!isAuthorized(CLLocationManager.authorizationStatus())) {
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
                val horizontalAccuracy = last.horizontalAccuracy.takeIf { it >= 0.0 }

                trySend(
                    FlightLocation(
                        latitude = last.coordinate.useContents { this.latitude },
                        longitude = last.coordinate.useContents { this.longitude },
                        altitudeMeters = last.altitude,
                        speedMetersPerSecond = speed,
                        bearingDegrees = bearing,
                        horizontalAccuracyMeters = horizontalAccuracy,
                        requiresPermission = false,
                        status = LocationStatus.Active
                    )
                )
            }

            override fun locationManager(
                manager: CLLocationManager,
                didFailWithError: NSError
            ) {
                println("[LocationServiceIOS] GPS signal lost: ${didFailWithError.localizedDescription}")
                sendSignalLost()
            }
        }

        manager.delegate = delegate
        retainedDelegate = delegate

        if (isAuthorized(CLLocationManager.authorizationStatus())) {
            manager.startUpdatingLocation()
        }

        awaitClose {
            manager.stopUpdatingLocation()
            manager.delegate = null
            retainedDelegate = null
        }
    }

    private fun isAuthorized(status: CLAuthorizationStatus): Boolean =
        status == kCLAuthorizationStatusAuthorizedAlways ||
        status == kCLAuthorizationStatusAuthorizedWhenInUse
}
