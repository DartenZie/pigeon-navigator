package cz.miroslavpasek.pigeonnavigator.di

import cz.miroslavpasek.pigeonnavigator.bridge.MapTapLookupCoordinator
import cz.miroslavpasek.pigeonnavigator.bridge.MapTapLookupState
import cz.miroslavpasek.pigeonnavigator.core.platform.coroutines.DispatcherProvider
import cz.miroslavpasek.pigeonnavigator.data.aviation.AviationPackageBootstrapper
import cz.miroslavpasek.pigeonnavigator.data.aviation.di.aviationDataModule
import cz.miroslavpasek.pigeonnavigator.data.search.di.searchDataModule
import cz.miroslavpasek.pigeonnavigator.data.terrain.di.terrainDataModule
import cz.miroslavpasek.pigeonnavigator.domain.terrain.AircraftSnapshot
import cz.miroslavpasek.pigeonnavigator.domain.terrain.TerrainHazardLevel
import cz.miroslavpasek.pigeonnavigator.feature.search.api.SearchStore
import cz.miroslavpasek.pigeonnavigator.feature.search.di.searchFeatureModule
import cz.miroslavpasek.pigeonnavigator.feature.search.presentation.SearchEffect
import cz.miroslavpasek.pigeonnavigator.feature.search.presentation.SearchIntent
import cz.miroslavpasek.pigeonnavigator.feature.search.presentation.SearchState
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.api.SearchDockStore
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.di.searchDockFeatureModule
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockIntent
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockRoute
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockState
import cz.miroslavpasek.pigeonnavigator.feature.terrainwarning.api.TerrainWarningStore
import cz.miroslavpasek.pigeonnavigator.feature.terrainwarning.di.terrainWarningFeatureModule
import cz.miroslavpasek.pigeonnavigator.feature.terrainwarning.presentation.TerrainWarningIntent
import cz.miroslavpasek.pigeonnavigator.feature.terrainwarning.presentation.TerrainWarningState
import cz.miroslavpasek.pigeonnavigator.services.LocationServiceConfig
import cz.miroslavpasek.pigeonnavigator.services.LocationStreamSource
import cz.miroslavpasek.pigeonnavigator.services.UdpLocationListenerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform
import org.koin.dsl.module
import kotlin.math.roundToInt

private class IosDispatcherProvider : DispatcherProvider {
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val io: CoroutineDispatcher = Dispatchers.Default
    override val default: CoroutineDispatcher = Dispatchers.Default
}

private val iosDispatcherModule = module {
    single<DispatcherProvider> { IosDispatcherProvider() }
}

private val iosMapTapLookupModule = module {
    factory {
        MapTapLookupCoordinator(
            queryNearbyAirportsUseCase = get(),
            queryContainingAirspacesUseCase = get(),
            dispatcherProvider = get()
        )
    }
}

private val iosLocationModule = module {
    single {
        LocationServiceConfig(
            source = LocationStreamSource.DeviceGps,
            udpListener = UdpLocationListenerConfig(
                ipAddress = "0.0.0.0",
                port = 49002,
            ),
        )
    }
}

/**
 * Starts Koin with shared and search feature modules for iOS.
 */
fun initKoin() {
    startKoin {
        modules(
            sharedModule,
            iosDispatcherModule,
            iosLocationModule,
            iosMapTapLookupModule,
            aviationDataModule(),
            searchDataModule(),
            terrainDataModule(),
            searchFeatureModule(),
            searchDockFeatureModule(),
            terrainWarningFeatureModule()
        )
    }

    runBlocking {
        KoinPlatform.getKoin().get<AviationPackageBootstrapper>().ensureInstalledFromAsset()
    }
}

/**
 * Resolves iOS-facing helper handles from Koin.
 */
class KoinHelper {
    /**
     * Returns a lifecycle-managed bridge over [SearchStore] for Swift UI layers.
     */
    fun getSearchStoreHandle(): SearchStoreHandle = SearchStoreHandle(KoinPlatform.getKoin().get())

    /** Returns a lifecycle-managed bridge over [MapTapLookupCoordinator] for Swift UI layers. */
    fun getMapTapLookupHandle(): MapTapLookupHandle = MapTapLookupHandle(KoinPlatform.getKoin().get())

    /** Returns a lifecycle-managed bridge over [TerrainWarningStore] for Swift UI layers. */
    fun getTerrainWarningHandle(): TerrainWarningHandle = TerrainWarningHandle(KoinPlatform.getKoin().get())

    /** Returns a lifecycle-managed bridge over [SearchDockStore] for Swift UI layers. */
    fun getSearchDockHandle(): SearchDockHandle = SearchDockHandle(KoinPlatform.getKoin().get())
}

/**
 * Bridges [SearchStore] flows and intents to a Swift-friendly API surface.
 */
class SearchStoreHandle(
    private val store: SearchStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var stateJob: Job? = null
    private var effectsJob: Job? = null

    /** Starts collecting state updates until [stopState] or [close] is called. */
    fun startState(onEach: (SearchState) -> Unit) {
        if (stateJob != null) return
        stateJob = scope.launch {
            store.state.collect { onEach(it) }
        }
    }

    /** Stops state collection started by [startState]. */
    fun stopState() {
        stateJob?.cancel()
        stateJob = null
    }

    /** Starts collecting one-shot effects until [stopEffects] or [close] is called. */
    fun startEffects(onEach: (SearchEffect) -> Unit) {
        if (effectsJob != null) return
        effectsJob = scope.launch {
            store.effects.collect { onEach(it) }
        }
    }

    /** Stops effect collection started by [startEffects]. */
    fun stopEffects() {
        effectsJob?.cancel()
        effectsJob = null
    }

    /** Forwards query changes to the underlying store. */
    fun onQueryChanged(query: String) {
        store.send(SearchIntent.QueryChanged(query))
    }

    /** Requests search execution for current query state. */
    fun submitSearch() {
        store.send(SearchIntent.SubmitSearch)
    }

    /** Requests clearing current query and results. */
    fun clearSearch() {
        store.send(SearchIntent.ClearSearch)
    }

    /** Stops all collection jobs and closes the underlying store. */
    fun close() {
        stopState()
        stopEffects()
        scope.cancel()
        store.close()
    }
}

/** Bridges [MapTapLookupCoordinator] state and queries to a Swift-friendly API surface. */
class MapTapLookupHandle(
    private val coordinator: MapTapLookupCoordinator
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var stateJob: Job? = null

    /** Starts collecting state updates until [stopState] or [close] is called. */
    fun startState(onEach: (MapTapLookupViewState) -> Unit) {
        if (stateJob != null) return
        stateJob = scope.launch {
            coordinator.state.collect { onEach(it.toViewState()) }
        }
    }

    /** Stops state collection started by [startState]. */
    fun stopState() {
        stateJob?.cancel()
        stateJob = null
    }

    /** Requests a lookup around the tapped coordinate. */
    fun queryAt(latitude: Double, longitude: Double) {
        coordinator.queryAt(latitude = latitude, longitude = longitude)
    }

    /** Stops active jobs and closes the underlying coordinator. */
    fun close() {
        stopState()
        scope.cancel()
        coordinator.close()
    }
}

/** Bridges [TerrainWarningStore] state and telemetry updates to a Swift-friendly API surface. */
class TerrainWarningHandle(
    private val store: TerrainWarningStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var stateJob: Job? = null

    /** Starts collecting terrain warning state updates until [stopState] or [close] is called. */
    fun startState(onEach: (TerrainWarningViewState) -> Unit) {
        if (stateJob != null) return
        stateJob = scope.launch {
            store.state.collect { onEach(it.toViewState()) }
        }
    }

    /** Stops state collection started by [startState]. */
    fun stopState() {
        stateJob?.cancel()
        stateJob = null
    }

    /** Sends aircraft telemetry to the shared terrain warning store. */
    fun onLocationUpdated(
        latitude: Double,
        longitude: Double,
        altitudeMeters: Double,
        speedMetersPerSecond: Double,
        bearingDegrees: Double
    ) {
        store.send(
            TerrainWarningIntent.LocationUpdated(
                snapshot = AircraftSnapshot(
                    latitude = latitude,
                    longitude = longitude,
                    altitudeMeters = altitudeMeters,
                    speedMetersPerSecond = speedMetersPerSecond,
                    bearingDegrees = bearingDegrees
                )
            )
        )
    }

    /** Stops active jobs and closes the underlying store. */
    fun close() {
        stopState()
        scope.cancel()
        store.close()
    }
}

/**
 * Bridges [SearchDockStore] state and intents to a Swift-friendly API surface.
 * Consolidates the HUD search bar routing that used to live in separate platform code.
 */
class SearchDockHandle(
    private val store: SearchDockStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var stateJob: Job? = null

    /** Starts collecting dock state updates until [stopState] or [close] is called. */
    fun startState(onEach: (SearchDockViewState) -> Unit) {
        if (stateJob != null) return
        stateJob = scope.launch {
            store.state.collect { onEach(it.toViewState()) }
        }
    }

    /** Stops state collection started by [startState]. */
    fun stopState() {
        stateJob?.cancel()
        stateJob = null
    }

    /** Forwards query changes to the underlying dock store. */
    fun onQueryChanged(query: String) {
        store.send(SearchDockIntent.SearchQueryChanged(query))
    }

    /** Triggers the shared search pipeline. */
    fun submitSearch() {
        store.send(SearchDockIntent.SubmitSearch)
    }

    /** Clears the current query and results. */
    fun clearSearch() {
        store.send(SearchDockIntent.SearchCleared)
    }

    /** Notifies the dock that the HUD expanded / collapsed. */
    fun onExpandedChanged(expanded: Boolean) {
        store.send(SearchDockIntent.ExpandedChanged(expanded = expanded))
    }

    /** Lets Swift manually select a route by tag (chip tap). */
    fun selectRoute(routeTag: String) {
        val route = routeTag.toSearchDockRoute() ?: return
        store.send(SearchDockIntent.RouteSelected(route = route))
    }

    /** Toggles the route planning mode on/off. */
    fun onRoutePlanningChanged(planning: Boolean) {
        store.send(SearchDockIntent.RoutePlanningChanged(planning = planning))
    }

    /** Updates whether the user has tapped a point on the map. */
    fun onMapSelectionChanged(hasSelection: Boolean) {
        store.send(SearchDockIntent.MapSelectionChanged(hasSelection = hasSelection))
    }

    /** Pushes the latest user location so nearby POIs can refresh. */
    fun onUserLocationChanged(latitude: Double, longitude: Double) {
        store.send(
            SearchDockIntent.UserLocationChanged(
                latitude = latitude,
                longitude = longitude
            )
        )
    }

    /** Stops active jobs and closes the underlying store. */
    fun close() {
        stopState()
        scope.cancel()
        store.close()
    }
}

data class SearchDockPoiViewItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val kindLabel: String,
    val distanceLabel: String
)

data class SearchDockViewState(
    val isExpanded: Boolean = false,
    val activeRouteTag: String = SearchDockRoute.Nearby.toTag(),
    val availableRouteTags: List<String> = SearchDockRoute.entries.map { it.toTag() },
    val isRoutePlanning: Boolean = false,
    val hasMapSelection: Boolean = false,
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<String> = emptyList(),
    val searchErrorMessage: String? = null,
    val isNearbyPoiLoading: Boolean = false,
    val nearbyPoiErrorMessage: String? = null,
    val nearbyPoiItems: List<SearchDockPoiViewItem> = emptyList()
)

private fun SearchDockRoute.toTag(): String = when (this) {
    SearchDockRoute.Nearby -> "nearby"
    SearchDockRoute.Search -> "search"
    SearchDockRoute.RoutePlanner -> "routePlanner"
    SearchDockRoute.MapTap -> "mapTap"
}

private fun String.toSearchDockRoute(): SearchDockRoute? = when (this) {
    "nearby" -> SearchDockRoute.Nearby
    "search" -> SearchDockRoute.Search
    "routePlanner" -> SearchDockRoute.RoutePlanner
    "mapTap" -> SearchDockRoute.MapTap
    else -> null
}

private fun SearchDockState.toViewState(): SearchDockViewState {
    return SearchDockViewState(
        isExpanded = isExpanded,
        activeRouteTag = activeRoute.toTag(),
        availableRouteTags = availableRoutes.map { it.toTag() },
        isRoutePlanning = isRoutePlanning,
        hasMapSelection = hasMapSelection,
        searchQuery = searchQuery,
        isSearching = isSearching,
        searchResults = searchResults,
        searchErrorMessage = searchErrorMessage,
        isNearbyPoiLoading = isNearbyPoiLoading,
        nearbyPoiErrorMessage = nearbyPoiErrorMessage,
        nearbyPoiItems = nearbyPoiItems.map {
            SearchDockPoiViewItem(
                id = it.id,
                title = it.title,
                subtitle = it.subtitle,
                kindLabel = it.kindLabel,
                distanceLabel = it.distanceMeters.toDistanceLabel()
            )
        }
    )
}

data class MapTapAirportItem(
    val id: String,
    val name: String,
    val distanceLabel: String
)

data class MapTapAirspaceItem(
    val id: String,
    val name: String,
    val detail: String
)

data class MapTapLookupViewState(
    val selectedLatitude: Double? = null,
    val selectedLongitude: Double? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val airports: List<MapTapAirportItem> = emptyList(),
    val airspaces: List<MapTapAirspaceItem> = emptyList()
)

data class TerrainHazardViewPoint(
    val latitude: Double,
    val longitude: Double,
    val severity: String
)

data class TerrainWarningViewState(
    val hazardPoints: List<TerrainHazardViewPoint> = emptyList()
)

private fun MapTapLookupState.toViewState(): MapTapLookupViewState {
    return MapTapLookupViewState(
        selectedLatitude = selectedLatitude,
        selectedLongitude = selectedLongitude,
        isLoading = isLoading,
        errorMessage = errorMessage,
        airports = airports.map {
            MapTapAirportItem(
                id = it.airport.id,
                name = it.airport.name,
                distanceLabel = it.distanceMeters.toDistanceLabel()
            )
        },
        airspaces = airspaces.map {
            MapTapAirspaceItem(
                id = it.id,
                name = it.name,
                detail = "${it.kind} · ${it.toAltitudeBand()}"
            )
        }
    )
}

private fun TerrainWarningState.toViewState(): TerrainWarningViewState {
    return TerrainWarningViewState(
        hazardPoints = prediction?.hazardSamples?.map {
            TerrainHazardViewPoint(
                latitude = it.latitude,
                longitude = it.longitude,
                severity = it.level.toSeverityTag()
            )
        } ?: emptyList()
    )
}

private fun TerrainHazardLevel.toSeverityTag(): String {
    return when (this) {
        TerrainHazardLevel.NearConflict -> "near"
        TerrainHazardLevel.Conflict -> "conflict"
    }
}

private fun Double.toDistanceLabel(): String {
    return if (this >= 1000.0) {
        "${(this / 1000.0 * 10.0).roundToInt() / 10.0} km"
    } else {
        "${roundToInt()} m"
    }
}

private fun cz.miroslavpasek.pigeonnavigator.domain.aviation.Airspace.toAltitudeBand(): String {
    val lower = lowerLimitMeters?.let { "${it}m ${lowerLimitReference.orEmpty()}".trim() } ?: "SFC"
    val upper = upperLimitMeters?.let { "${it}m ${upperLimitReference.orEmpty()}".trim() } ?: "UNL"
    return "$lower - $upper"
}
