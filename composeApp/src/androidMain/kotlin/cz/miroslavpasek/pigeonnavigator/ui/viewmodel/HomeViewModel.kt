package cz.miroslavpasek.pigeonnavigator.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.miroslavpasek.pigeonnavigator.data.FlightLocation
import cz.miroslavpasek.pigeonnavigator.services.LocationService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HomeUIState(
    val location: FlightLocation? = null,
    val requiresPermission: Boolean = false
)

class HomeViewModel(
    private val locationService: LocationService
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUIState())
    val uiState: StateFlow<HomeUIState> = _uiState

    private var locationJob: Job? = null

    init {
        startLocationUpdates()
    }

    fun refreshLocation() {
        locationJob?.cancel()
        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        locationJob = viewModelScope.launch {
            locationService.observeLocationUpdates().collect { loc ->
                _uiState.value = HomeUIState(
                    location = loc,
                    requiresPermission = loc.requiresPermission
                )
            }
        }
    }
}