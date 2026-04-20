package cz.miroslavpasek.pigeonnavigator.services

import cz.miroslavpasek.pigeonnavigator.data.FlightLocation
import kotlinx.coroutines.flow.Flow

/**
 * Delegates location updates to GPS or UDP debug source based on config.
 */
class ConfigurableLocationService(
    private val config: LocationServiceConfig,
    private val gpsLocationService: LocationService,
    private val udpLocationService: LocationService,
) : LocationService {
    override fun observeLocationUpdates(): Flow<FlightLocation> = when (config.source) {
        LocationStreamSource.DeviceGps -> gpsLocationService.observeLocationUpdates()
        LocationStreamSource.UdpDebug -> udpLocationService.observeLocationUpdates()
    }
}
