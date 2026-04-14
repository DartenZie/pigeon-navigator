package cz.miroslavpasek.pigeonnavigator.di

import cz.miroslavpasek.pigeonnavigator.bridge.MapTapLookupCoordinator
import cz.miroslavpasek.pigeonnavigator.bridge.MapTapLookupState
import cz.miroslavpasek.pigeonnavigator.core.platform.coroutines.DispatcherProvider
import cz.miroslavpasek.pigeonnavigator.data.aviation.AviationPackageBootstrapper
import cz.miroslavpasek.pigeonnavigator.data.aviation.di.aviationDataModule
import cz.miroslavpasek.pigeonnavigator.data.search.di.searchDataModule
import cz.miroslavpasek.pigeonnavigator.feature.search.api.SearchStore
import cz.miroslavpasek.pigeonnavigator.feature.search.di.searchFeatureModule
import cz.miroslavpasek.pigeonnavigator.feature.search.presentation.SearchEffect
import cz.miroslavpasek.pigeonnavigator.feature.search.presentation.SearchIntent
import cz.miroslavpasek.pigeonnavigator.feature.search.presentation.SearchState
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

/**
 * Starts Koin with shared and search feature modules for iOS.
 */
fun initKoin() {
    startKoin {
        modules(
            sharedModule,
            iosDispatcherModule,
            iosMapTapLookupModule,
            aviationDataModule(),
            searchDataModule(),
            searchFeatureModule()
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
