package cz.miroslavpasek.pigeonnavigator.platform

import cz.miroslavpasek.pigeonnavigator.services.LocationService
import cz.miroslavpasek.pigeonnavigator.services.LocationServiceIOS

actual fun createLocationService(): LocationService = LocationServiceIOS()
