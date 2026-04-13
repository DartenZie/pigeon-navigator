package cz.miroslavpasek.pigeonnavigator.platform

import cz.miroslavpasek.pigeonnavigator.services.LocationService
import cz.miroslavpasek.pigeonnavigator.services.LocationServiceIOS

/**
 * Creates the iOS [LocationService] backed by CoreLocation.
 */
actual fun createLocationService(): LocationService = LocationServiceIOS()
