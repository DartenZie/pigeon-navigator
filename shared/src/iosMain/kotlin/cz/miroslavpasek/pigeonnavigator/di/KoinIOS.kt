package cz.miroslavpasek.pigeonnavigator.di

import cz.miroslavpasek.pigeonnavigator.bridge.MapTapLookupCoordinator
import cz.miroslavpasek.pigeonnavigator.bridge.MapTapLookupState
import cz.miroslavpasek.pigeonnavigator.core.platform.coroutines.DispatcherProvider
import cz.miroslavpasek.pigeonnavigator.core.platform.settings.KeyValueSettingsStore
import cz.miroslavpasek.pigeonnavigator.data.aviation.AviationPackageBootstrapper
import cz.miroslavpasek.pigeonnavigator.data.aviation.di.aviationDataModule
import cz.miroslavpasek.pigeonnavigator.data.search.di.searchDataModule
import cz.miroslavpasek.pigeonnavigator.data.settings.di.settingsDataModule
import cz.miroslavpasek.pigeonnavigator.data.terrain.di.terrainDataModule
import cz.miroslavpasek.pigeonnavigator.domain.settings.AppSettings
import cz.miroslavpasek.pigeonnavigator.domain.settings.AppSettingsRepository
import cz.miroslavpasek.pigeonnavigator.domain.settings.MapPreferences
import cz.miroslavpasek.pigeonnavigator.domain.settings.SearchPreferences
import cz.miroslavpasek.pigeonnavigator.domain.settings.UnitPreferences
import cz.miroslavpasek.pigeonnavigator.domain.settings.WarningPreferences
import cz.miroslavpasek.pigeonnavigator.platform.IosKeyValueSettingsStore
import cz.miroslavpasek.pigeonnavigator.domain.aviation.GeoPoint
import cz.miroslavpasek.pigeonnavigator.domain.search.GeoBounds
import cz.miroslavpasek.pigeonnavigator.domain.search.SearchResult
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
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockRoutePoint
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
            queryNearbyNavaidsUseCase = get(),
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

private val iosSettingsModule = module {
    single<KeyValueSettingsStore> { IosKeyValueSettingsStore() }
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
            iosSettingsModule,
            settingsDataModule(),
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

    /** Returns a lifecycle-managed bridge over [AppSettingsRepository] for Swift UI layers. */
    fun getSettingsHandle(): SettingsHandle = SettingsHandle(KoinPlatform.getKoin().get())
}

/**
 * Bridges [AppSettingsRepository] flows and update calls to a Swift-friendly API surface.
 *
 * Provides start/stop semantics so Swift can subscribe and unsubscribe lifecycle-aware,
 * along with grouped update entry points that match the domain repository contract.
 */
class SettingsHandle(
    private val repository: AppSettingsRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var stateJob: Job? = null

    /** Starts collecting settings updates until [stopSettings] or [close] is called. */
    fun startSettings(onEach: (AppSettings) -> Unit) {
        if (stateJob != null) return
        stateJob = scope.launch {
            repository.settings.collect { onEach(it) }
        }
    }

    /** Stops settings collection started by [startSettings]. */
    fun stopSettings() {
        stateJob?.cancel()
        stateJob = null
    }

    /**
     * Forwards a unit-preference update to the shared repository.
     *
     * Returns `true` when persistence succeeded.
     */
    fun updateUnits(units: UnitPreferences, onResult: (Boolean) -> Unit) {
        scope.launch {
            val result = repository.updateUnits(units)
            onResult(result is cz.miroslavpasek.pigeonnavigator.core.util.result.AppResult.Success)
        }
    }

    /**
     * Forwards a time-to-collision threshold update to the shared repository.
     *
     * Returns `true` when persistence succeeded; validation failures resolve to `false`
     * and leave the persisted value unchanged.
     */
    fun updateTimeToCollisionWarningSeconds(seconds: Int, onResult: (Boolean) -> Unit) {
        scope.launch {
            val result = repository.updateWarningPreferences(
                WarningPreferences(timeToCollisionWarningSeconds = seconds)
            )
            onResult(result is cz.miroslavpasek.pigeonnavigator.core.util.result.AppResult.Success)
        }
    }

    /**
     * Forwards a search-preferences update (debounce + minimum query length) to the repository.
     *
     * Returns `true` when persistence succeeded; validation failures resolve to `false`.
     */
    fun updateSearchPreferences(
        searchDebounceMillis: Long,
        minimumQueryLength: Int,
        onResult: (Boolean) -> Unit
    ) {
        scope.launch {
            val result = repository.updateSearchPreferences(
                SearchPreferences(
                    searchDebounceMillis = searchDebounceMillis,
                    minimumQueryLength = minimumQueryLength
                )
            )
            onResult(result is cz.miroslavpasek.pigeonnavigator.core.util.result.AppResult.Success)
        }
    }

    /**
     * Forwards a map-preferences update to the repository.
     *
     * Returns `true` when persistence succeeded; validation failures resolve to `false`.
     */
    fun updateMapPreferences(
        maxDynamicZoomSpeedKmh: Double,
        maxSpeedZoomOutDelta: Double,
        bearingUpdateThresholdDegrees: Double,
        onResult: (Boolean) -> Unit
    ) {
        scope.launch {
            val result = repository.updateMapPreferences(
                MapPreferences(
                    maxDynamicZoomSpeedKmh = maxDynamicZoomSpeedKmh,
                    maxSpeedZoomOutDelta = maxSpeedZoomOutDelta,
                    bearingUpdateThresholdDegrees = bearingUpdateThresholdDegrees
                )
            )
            onResult(result is cz.miroslavpasek.pigeonnavigator.core.util.result.AppResult.Success)
        }
    }

    /** Stops active jobs. */
    fun close() {
        stopSettings()
        scope.cancel()
    }
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

    /**
     * Forwards the latest map-tap lookup state. The shared reducer auto-expands
     * the dock and switches to the MapTap route when a fresh lookup
     * (a previously-unseen [cursor]) completes with at least one result.
     */
    fun onMapTapLookupChanged(
        cursor: Long,
        isLoading: Boolean,
        hasResults: Boolean,
        hasSelection: Boolean
    ) {
        store.send(
            SearchDockIntent.MapTapLookupChanged(
                cursor = cursor,
                isLoading = isLoading,
                hasResults = hasResults,
                hasSelection = hasSelection
            )
        )
    }

    /** Opens the map-tap detail panel for the given key (e.g. `"airport:LKAA"`). */
    fun openMapTapDetail(key: String) {
        store.send(SearchDockIntent.OpenMapTapDetail(key = key))
    }

    /** Dismisses the map-tap detail panel and returns to the list view. */
    fun closeMapTapDetail() {
        store.send(SearchDockIntent.CloseMapTapDetail)
    }

    /** Pushes the latest user location so nearby POIs can refresh. */
    fun onUserLocationChanged(latitude: Double, longitude: Double, speedMetersPerSecond: Double? = null) {
        store.send(
            SearchDockIntent.UserLocationChanged(
                latitude = latitude,
                longitude = longitude,
                speedMetersPerSecond = speedMetersPerSecond
            )
        )
    }

    /** Adds a route destination and opens the shared route planner. */
    fun addRouteDestination(
        id: String,
        title: String,
        latitude: Double,
        longitude: Double
    ) {
        store.send(
            SearchDockIntent.RouteDestinationAdded(
                SearchDockRoutePoint(
                    id = id,
                    title = title,
                    latitude = latitude,
                    longitude = longitude
                )
            )
        )
    }

    /** Removes a destination from the shared route planner. */
    fun removeRouteDestination(id: String) {
        store.send(SearchDockIntent.RouteDestinationRemoved(id = id))
    }

    fun openNavigationDetail() {
        store.send(SearchDockIntent.OpenNavigationDetail)
    }

    fun openNavigationWaypointDetail(id: String) {
        store.send(SearchDockIntent.OpenNavigationWaypointDetail(id = id))
    }

    fun closeNavigationDetail() {
        store.send(SearchDockIntent.CloseNavigationDetail)
    }

    fun addWaypoint() {
        store.send(SearchDockIntent.AddWaypointRequested)
    }

    fun endFlight() {
        store.send(SearchDockIntent.EndFlight)
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
    val frequency: String? = null,
    val distanceLabel: String,
    val latitude: Double,
    val longitude: Double
)

data class SearchDockResultViewItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val kindLabel: String,
    val frequency: String? = null,
    val latitude: Double,
    val longitude: Double,
    val minLatitude: Double? = null,
    val minLongitude: Double? = null,
    val maxLatitude: Double? = null,
    val maxLongitude: Double? = null
)

data class SearchDockRoutePointViewItem(
    val id: String,
    val title: String,
    val latitude: Double,
    val longitude: Double
)

data class SearchDockViewState(
    val isExpanded: Boolean = false,
    val activeRouteTag: String = SearchDockRoute.Nearby.toTag(),
    val availableRouteTags: List<String> = SearchDockRoute.entries.map { it.toTag() },
    val isRoutePlanning: Boolean = false,
    val hasMapSelection: Boolean = false,
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<SearchDockResultViewItem> = emptyList(),
    val searchErrorMessage: String? = null,
    val isNearbyPoiLoading: Boolean = false,
    val nearbyPoiErrorMessage: String? = null,
    val nearbyPoiItems: List<SearchDockPoiViewItem> = emptyList(),
    val routeDestinations: List<SearchDockRoutePointViewItem> = emptyList(),
    val selectedMapTapDetailKey: String? = null,
    val isNavigating: Boolean = false,
    val navigationSummary: SearchDockNavigationSummaryViewItem? = null,
    val nextWaypoint: SearchDockRoutePointViewItem? = null,
    val selectedNavigationWaypoint: SearchDockRoutePointViewItem? = null,
    val headerModeTag: String = "search"
)

data class SearchDockNavigationSummaryViewItem(
    val remainingDistanceMeters: Double,
    val remainingSeconds: Long?,
    val speedMetersPerSecond: Double?
)

private fun SearchDockRoute.toTag(): String = when (this) {
    SearchDockRoute.Nearby -> "nearby"
    SearchDockRoute.Search -> "search"
    SearchDockRoute.RoutePlanner -> "routePlanner"
    SearchDockRoute.MapTap -> "mapTap"
    SearchDockRoute.NavigationDetail -> "navigationDetail"
}

private fun String.toSearchDockRoute(): SearchDockRoute? = when (this) {
    "nearby" -> SearchDockRoute.Nearby
    "search" -> SearchDockRoute.Search
    "routePlanner" -> SearchDockRoute.RoutePlanner
    "mapTap" -> SearchDockRoute.MapTap
    "navigationDetail" -> SearchDockRoute.NavigationDetail
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
        searchResults = searchResults.map { it.toViewItem() },
        searchErrorMessage = searchErrorMessage,
        isNearbyPoiLoading = isNearbyPoiLoading,
        nearbyPoiErrorMessage = nearbyPoiErrorMessage,
        nearbyPoiItems = nearbyPoiItems.map {
            SearchDockPoiViewItem(
                id = it.id,
                title = it.title,
                subtitle = it.subtitle,
                kindLabel = it.kindLabel,
                frequency = it.frequency,
                distanceLabel = it.distanceMeters.toDistanceLabel(),
                latitude = it.latitude,
                longitude = it.longitude
            )
        },
        routeDestinations = routeDestinations.map {
            SearchDockRoutePointViewItem(
                id = it.id,
                title = it.title,
                latitude = it.latitude,
                longitude = it.longitude
            )
        },
        selectedMapTapDetailKey = selectedMapTapDetailKey,
        isNavigating = isNavigating,
        navigationSummary = navigationSummary?.let {
            SearchDockNavigationSummaryViewItem(
                remainingDistanceMeters = it.remainingDistanceMeters,
                remainingSeconds = it.remainingSeconds,
                speedMetersPerSecond = it.speedMetersPerSecond
            )
        },
        nextWaypoint = nextWaypoint?.toViewItem(),
        selectedNavigationWaypoint = selectedNavigationWaypoint?.toViewItem(),
        headerModeTag = headerMode.name.lowercase()
    )
}

private fun SearchDockRoutePoint.toViewItem(): SearchDockRoutePointViewItem {
    return SearchDockRoutePointViewItem(
        id = id,
        title = title,
        latitude = latitude,
        longitude = longitude
    )
}

data class MapTapAirportItem(
    val id: String,
    val name: String,
    val distanceLabel: String,
    val latitude: Double,
    val longitude: Double
)

data class MapTapAirspaceItem(
    val id: String,
    val name: String,
    val detail: String,
    val latitude: Double,
    val longitude: Double,
    val minLatitude: Double,
    val minLongitude: Double,
    val maxLatitude: Double,
    val maxLongitude: Double
)

data class MapTapNavaidItem(
    val id: String,
    val ident: String,
    val name: String,
    val detail: String,
    val frequency: String?,
    val distanceLabel: String,
    val latitude: Double,
    val longitude: Double
)

data class MapTapLookupViewState(
    val selectedLatitude: Double? = null,
    val selectedLongitude: Double? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val airports: List<MapTapAirportItem> = emptyList(),
    val airspaces: List<MapTapAirspaceItem> = emptyList(),
    val navaids: List<MapTapNavaidItem> = emptyList(),
    val lookupSequence: Long = 0L
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
                distanceLabel = it.distanceMeters.toDistanceLabel(),
                latitude = it.airport.latitude,
                longitude = it.airport.longitude
            )
        },
        airspaces = airspaces.map {
            val bounds = it.points.toBounds()
                ?: GeoBounds(
                    minLatitude = 0.0,
                    minLongitude = 0.0,
                    maxLatitude = 0.0,
                    maxLongitude = 0.0
                )
            MapTapAirspaceItem(
                id = it.id,
                name = it.name,
                detail = "${it.kind} · ${it.toAltitudeBand()}",
                latitude = bounds.center.latitude,
                longitude = bounds.center.longitude,
                minLatitude = bounds.minLatitude,
                minLongitude = bounds.minLongitude,
                maxLatitude = bounds.maxLatitude,
                maxLongitude = bounds.maxLongitude
            )
        },
        navaids = navaids.map {
            MapTapNavaidItem(
                id = "navaid:${it.navaid.id}",
                ident = it.navaid.id,
                name = it.navaid.name,
                detail = "${it.navaid.kind} · ${it.navaid.detail}",
                frequency = it.navaid.frequency,
                distanceLabel = it.distanceMeters.toDistanceLabel(),
                latitude = it.navaid.latitude,
                longitude = it.navaid.longitude
            )
        },
        lookupSequence = lookupSequence
    )
}

private fun SearchResult.toViewItem(): SearchDockResultViewItem {
    return when (this) {
        is SearchResult.Airport -> SearchDockResultViewItem(
            id = id,
            title = title,
            subtitle = subtitle,
            kindLabel = kindLabel,
            latitude = latitude,
            longitude = longitude
        )

        is SearchResult.Navaid -> SearchDockResultViewItem(
            id = id,
            title = title,
            subtitle = subtitle,
            kindLabel = kindLabel,
            frequency = frequency,
            latitude = latitude,
            longitude = longitude
        )

        is SearchResult.Airspace -> SearchDockResultViewItem(
            id = id,
            title = title,
            subtitle = subtitle,
            kindLabel = kindLabel,
            latitude = centerLatitude,
            longitude = centerLongitude,
            minLatitude = bounds.minLatitude,
            minLongitude = bounds.minLongitude,
            maxLatitude = bounds.maxLatitude,
            maxLongitude = bounds.maxLongitude
        )
    }
}

private fun List<GeoPoint>.toBounds(): GeoBounds? {
    if (isEmpty()) return null
    var minLatitude = first().latitude
    var maxLatitude = first().latitude
    var minLongitude = first().longitude
    var maxLongitude = first().longitude
    for (point in drop(1)) {
        minLatitude = minOf(minLatitude, point.latitude)
        maxLatitude = maxOf(maxLatitude, point.latitude)
        minLongitude = minOf(minLongitude, point.longitude)
        maxLongitude = maxOf(maxLongitude, point.longitude)
    }
    return GeoBounds(
        minLatitude = minLatitude,
        minLongitude = minLongitude,
        maxLatitude = maxLatitude,
        maxLongitude = maxLongitude
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
