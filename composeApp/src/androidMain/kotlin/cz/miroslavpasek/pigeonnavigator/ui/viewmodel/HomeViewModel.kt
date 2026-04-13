package cz.miroslavpasek.pigeonnavigator.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.miroslavpasek.pigeonnavigator.data.FlightLocation
import cz.miroslavpasek.pigeonnavigator.domain.terrain.AircraftSnapshot
import cz.miroslavpasek.pigeonnavigator.domain.terrain.TerrainWarningLevel
import cz.miroslavpasek.pigeonnavigator.feature.terrainwarning.api.TerrainWarningStore
import cz.miroslavpasek.pigeonnavigator.feature.terrainwarning.presentation.TerrainWarningIntent
import cz.miroslavpasek.pigeonnavigator.services.LocationService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUIState(
    val location: FlightLocation? = null,
    val requiresPermission: Boolean = false,
    val terrainWarningLevel: TerrainWarningLevel = TerrainWarningLevel.None,
    val terrainDistanceToImpactMeters: Double? = null
)

class HomeViewModel(
    private val locationService: LocationService,
    private val terrainWarningStore: TerrainWarningStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUIState())
    val uiState: StateFlow<HomeUIState> = _uiState

    private var locationJob: Job? = null
    private var terrainStateJob: Job? = null

    init {
        observeTerrainWarnings()
        startLocationUpdates()
    }

    fun refreshLocation() {
        locationJob?.cancel()
        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        locationJob = viewModelScope.launch {
            locationService.observeLocationUpdates().collect { loc ->
                terrainWarningStore.send(
                    TerrainWarningIntent.LocationUpdated(
                        AircraftSnapshot(
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            altitudeMeters = loc.altitudeMeters,
                            speedMetersPerSecond = loc.speedMetersPerSecond.toDouble(),
                            bearingDegrees = loc.bearingDegrees.toDouble()
                        )
                    )
                )

                _uiState.update {
                    it.copy(
                        location = loc,
                        requiresPermission = loc.requiresPermission
                    )
                }
            }
        }
    }

    private fun observeTerrainWarnings() {
        terrainStateJob = viewModelScope.launch {
            terrainWarningStore.state.collect { warningState ->
                _uiState.update {
                    it.copy(
                        terrainWarningLevel = warningState.warningLevel,
                        terrainDistanceToImpactMeters = warningState.prediction?.distanceToImpactMeters
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        locationJob?.cancel()
        terrainStateJob?.cancel()
        terrainWarningStore.close()
    }
}
