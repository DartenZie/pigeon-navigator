package cz.miroslavpasek.pigeonnavigator.services

import cz.miroslavpasek.pigeonnavigator.data.FlightLocation
import kotlinx.coroutines.flow.Flow

interface LocationService {
    fun observeLocationUpdates(): Flow<FlightLocation>
}
