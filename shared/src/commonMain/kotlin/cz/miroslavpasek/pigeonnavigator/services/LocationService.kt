package cz.miroslavpasek.pigeonnavigator.services

import cz.miroslavpasek.pigeonnavigator.data.FlightLocation
import kotlinx.coroutines.flow.Flow

/**
 * Streams aircraft location updates produced by platform APIs.
 */
interface LocationService {
    /**
     * Returns a cold [Flow] of location updates.
     */
    fun observeLocationUpdates(): Flow<FlightLocation>
}
