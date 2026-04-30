package cz.miroslavpasek.pigeonnavigator.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.miroslavpasek.pigeonnavigator.bridge.AirspaceWarningCoordinator
import cz.miroslavpasek.pigeonnavigator.bridge.AirspaceWarningState
import cz.miroslavpasek.pigeonnavigator.bridge.MapTapLookupCoordinator
import cz.miroslavpasek.pigeonnavigator.bridge.MapTapLookupState
import cz.miroslavpasek.pigeonnavigator.data.FlightLocation
import cz.miroslavpasek.pigeonnavigator.data.LocationStatus
import cz.miroslavpasek.pigeonnavigator.domain.terrain.TerrainHazardSample
import cz.miroslavpasek.pigeonnavigator.domain.terrain.AircraftSnapshot
import cz.miroslavpasek.pigeonnavigator.domain.terrain.TerrainWarningLevel
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.api.SearchDockStore
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockIntent
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockRoute
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockRoutePoint
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockState
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
    val locationStatus: LocationStatus? = null,
    val terrainWarningLevel: TerrainWarningLevel = TerrainWarningLevel.None,
    val terrainDistanceToImpactMeters: Double? = null,
    val isTerrainCollisionWithinOneMinute: Boolean = false,
    val airspaceWarning: AirspaceWarningState = AirspaceWarningState(),
    val terrainHazardSamples: List<TerrainHazardSample> = emptyList(),
    val mapTapLookup: MapTapLookupState = MapTapLookupState(),
    val searchDock: SearchDockState = SearchDockState()
)

class HomeViewModel(
    private val locationService: LocationService,
    private val terrainWarningStore: TerrainWarningStore,
    private val airspaceWarningCoordinator: AirspaceWarningCoordinator,
    private val mapTapLookupCoordinator: MapTapLookupCoordinator,
    private val searchDockStore: SearchDockStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUIState())
    val uiState: StateFlow<HomeUIState> = _uiState

    private var locationJob: Job? = null
    private var terrainStateJob: Job? = null
    private var airspaceWarningStateJob: Job? = null
    private var searchDockStateJob: Job? = null

    init {
        observeTerrainWarnings()
        observeAirspaceWarnings()
        observeMapTapLookup()
        observeSearchDockState()
        startLocationUpdates()
    }

    fun onMapTapped(latitude: Double, longitude: Double) {
        println("[HomeViewModel] onMapTapped lat=$latitude lng=$longitude")
        mapTapLookupCoordinator.queryAt(latitude = latitude, longitude = longitude)
        collapseSearchDockIfExpanded()
    }

    fun onMapInteracted() {
        collapseSearchDockIfExpanded()
    }

    fun onSearchDockExpandedChanged(expanded: Boolean) {
        searchDockStore.send(SearchDockIntent.ExpandedChanged(expanded = expanded))
    }

    fun onSearchDockQueryChanged(query: String) {
        searchDockStore.send(SearchDockIntent.SearchQueryChanged(query = query))
    }

    fun onSearchDockSubmitSearch() {
        searchDockStore.send(SearchDockIntent.SubmitSearch)
    }

    fun onSearchDockClearSearch() {
        searchDockStore.send(SearchDockIntent.SearchCleared)
    }

    fun onSearchDockRoutePlanningChanged(planning: Boolean) {
        searchDockStore.send(SearchDockIntent.RoutePlanningChanged(planning = planning))
    }

    fun onSearchDockRouteSelected(route: SearchDockRoute) {
        searchDockStore.send(SearchDockIntent.RouteSelected(route = route))
    }

    fun onRouteDestinationAdded(point: SearchDockRoutePoint) {
        searchDockStore.send(SearchDockIntent.RouteDestinationAdded(point = point))
    }

    fun onRouteDestinationRemoved(id: String) {
        searchDockStore.send(SearchDockIntent.RouteDestinationRemoved(id = id))
    }

    fun onMapTapDetailRequested(key: String) {
        searchDockStore.send(SearchDockIntent.OpenMapTapDetail(key = key))
    }

    fun onMapTapDetailClosed() {
        searchDockStore.send(SearchDockIntent.CloseMapTapDetail)
    }

    fun onNavigationDetailRequested() {
        searchDockStore.send(SearchDockIntent.OpenNavigationDetail)
    }

    fun onNavigationWaypointDetailRequested(id: String) {
        searchDockStore.send(SearchDockIntent.OpenNavigationWaypointDetail(id = id))
    }

    fun onAddWaypointRequested() {
        searchDockStore.send(SearchDockIntent.AddWaypointRequested)
    }

    fun onEndFlightRequested() {
        searchDockStore.send(SearchDockIntent.EndFlight)
    }

    fun collapseSearchDock() {
        collapseSearchDockIfExpanded()
    }

    fun refreshLocation() {
        locationJob?.cancel()
        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        locationJob = viewModelScope.launch {
            locationService.observeLocationUpdates().collect { loc ->
                if (loc.status == LocationStatus.Active) {
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

                    searchDockStore.send(
                        SearchDockIntent.UserLocationChanged(
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            speedMetersPerSecond = loc.speedMetersPerSecond.toDouble()
                        )
                    )
                    airspaceWarningCoordinator.onLocationUpdated(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        speedMetersPerSecond = loc.speedMetersPerSecond.toDouble(),
                        bearingDegrees = loc.bearingDegrees.toDouble()
                    )
                }

                _uiState.update {
                    it.copy(
                        location = if (loc.status == LocationStatus.Active) loc else it.location,
                        requiresPermission = loc.status == LocationStatus.PermissionRequired,
                        locationStatus = loc.status
                    )
                }
            }
        }
    }

    private fun observeTerrainWarnings() {
        terrainStateJob = viewModelScope.launch {
            terrainWarningStore.state.collect { warningState ->
                _uiState.update {
                    val hazardSamples = if (warningState.warningLevel == TerrainWarningLevel.None) {
                        emptyList()
                    } else {
                        warningState.prediction?.hazardSamples ?: emptyList()
                    }

                    it.copy(
                        terrainWarningLevel = warningState.warningLevel,
                        terrainDistanceToImpactMeters = warningState.prediction?.distanceToImpactMeters,
                        isTerrainCollisionWithinOneMinute = warningState.prediction?.let { prediction ->
                            prediction.hasConflict && prediction.timeToImpactSeconds?.let { it <= 60.0 } == true
                        } ?: false,
                        terrainHazardSamples = hazardSamples
                    )
                }
            }
        }
    }

    private fun observeAirspaceWarnings() {
        airspaceWarningStateJob = viewModelScope.launch {
            airspaceWarningCoordinator.state.collect { warningState ->
                _uiState.update {
                    it.copy(airspaceWarning = warningState)
                }
            }
        }
    }

    private fun collapseSearchDockIfExpanded() {
        if (_uiState.value.searchDock.isExpanded) {
            searchDockStore.send(SearchDockIntent.ExpandedChanged(expanded = false))
        }
    }

    private fun observeMapTapLookup() {
        viewModelScope.launch {
            mapTapLookupCoordinator.state.collect { lookupState ->
                val hasSelection = lookupState.selectedLatitude != null && lookupState.selectedLongitude != null
                val hasResults = lookupState.airports.isNotEmpty() ||
                    lookupState.airspaces.isNotEmpty() ||
                    lookupState.navaids.isNotEmpty()
                searchDockStore.send(
                    SearchDockIntent.MapTapLookupChanged(
                        cursor = lookupState.lookupSequence,
                        isLoading = lookupState.isLoading,
                        hasResults = hasResults,
                        hasSelection = hasSelection
                    )
                )
                _uiState.update {
                    it.copy(mapTapLookup = lookupState)
                }
            }
        }
    }

    private fun observeSearchDockState() {
        searchDockStateJob = viewModelScope.launch {
            searchDockStore.state.collect { dockState ->
                _uiState.update {
                    it.copy(searchDock = dockState)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        locationJob?.cancel()
        terrainStateJob?.cancel()
        airspaceWarningStateJob?.cancel()
        searchDockStateJob?.cancel()
        airspaceWarningCoordinator.close()
        mapTapLookupCoordinator.close()
        searchDockStore.close()
        terrainWarningStore.close()
    }
}
