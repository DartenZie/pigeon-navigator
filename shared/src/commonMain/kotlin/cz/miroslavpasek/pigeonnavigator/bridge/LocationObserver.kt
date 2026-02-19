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

class LocationObserver : KoinComponent {
    private val service: LocationService by inject()
    private var job: Job? = null

    fun start(onUpdate: (FlightLocation) -> Unit) {
        if (job != null) return
        job = CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
            service.observeLocationUpdates().collect { onUpdate(it) }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}