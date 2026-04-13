package cz.miroslavpasek.pigeonnavigator.platform

import cz.miroslavpasek.pigeonnavigator.services.LocationService

/**
 * Creates the platform-specific [LocationService] implementation.
 */
expect fun createLocationService(): LocationService
