package cz.miroslavpasek.pigeonnavigator.bridge

import cz.miroslavpasek.pigeonnavigator.data.FlightLocation
import cz.miroslavpasek.pigeonnavigator.services.LocationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Bridges [LocationService] flow updates to callback-based platform consumers.
 *
 * Call [start] once to begin observation and [stop] to release the active collection job.
 */
class LocationObserver : KoinComponent {
    private val service: LocationService by inject()
    private var job: Job? = null

    /**
     * Starts location observation and forwards each update to [onUpdate].
     */
    fun start(onUpdate: (FlightLocation) -> Unit) {
        if (job != null) return
        job = CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
            service.observeLocationUpdates().collect { onUpdate(it) }
        }
    }

    /**
     * Stops active observation if running.
     */
    fun stop() {
        job?.cancel()
        job = null
    }
}
