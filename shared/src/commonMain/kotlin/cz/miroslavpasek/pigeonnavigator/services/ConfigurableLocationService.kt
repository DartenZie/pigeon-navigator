package cz.miroslavpasek.pigeonnavigator.services

import cz.miroslavpasek.pigeonnavigator.data.FlightLocation
import cz.miroslavpasek.pigeonnavigator.domain.settings.AppSettingsRepository
import cz.miroslavpasek.pigeonnavigator.domain.settings.LocationSource
import cz.miroslavpasek.pigeonnavigator.domain.settings.UdpLocationFormat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/**
 * Delegates location updates to GPS or UDP source based on persisted settings.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConfigurableLocationService(
    private val settingsRepository: AppSettingsRepository,
    private val gpsLocationService: LocationService,
    private val msfsUdpLocationService: LocationService,
    private val xplaneUdpLocationService: LocationService,
) : LocationService {
    override fun observeLocationUpdates(): Flow<FlightLocation> = settingsRepository.settings
        .map { it.location }
        .distinctUntilChanged()
        .flatMapLatest { location ->
            when (location.source) {
                LocationSource.DeviceGps -> gpsLocationService.observeLocationUpdates()
                LocationSource.Udp -> when (location.udpFormat) {
                    UdpLocationFormat.Msfs -> msfsUdpLocationService.observeLocationUpdates()
                    UdpLocationFormat.XPlane -> xplaneUdpLocationService.observeLocationUpdates()
                }
            }
        }
}
