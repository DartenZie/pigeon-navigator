package cz.miroslavpasek.pigeonnavigator.feature.searchdock.internal

import cz.miroslavpasek.pigeonnavigator.core.platform.coroutines.DispatcherProvider
import cz.miroslavpasek.pigeonnavigator.core.util.result.AppResult
import cz.miroslavpasek.pigeonnavigator.domain.aviation.NearbyAirport
import cz.miroslavpasek.pigeonnavigator.domain.aviation.NearbyNavaid
import cz.miroslavpasek.pigeonnavigator.domain.aviation.QueryNearbyAirportsUseCase
import cz.miroslavpasek.pigeonnavigator.domain.aviation.QueryNearbyNavaidsUseCase
import cz.miroslavpasek.pigeonnavigator.domain.failure.Failure
import cz.miroslavpasek.pigeonnavigator.domain.search.SearchUseCase
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.api.SearchDockStore
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockEffect
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockIntent
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockNavigationSummary
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockPoiItem
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockReducer
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockRoute
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockRoutePoint
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

internal class RealSearchDockStore(
    private val searchUseCase: SearchUseCase,
    private val queryNearbyAirportsUseCase: QueryNearbyAirportsUseCase,
    private val queryNearbyNavaidsUseCase: QueryNearbyNavaidsUseCase,
    private val reducer: SearchDockReducer,
    private val dispatcherProvider: DispatcherProvider,
    private val minNearbyPoiRefetchDistanceMeters: Double = MIN_NEARBY_POI_REFETCH_DISTANCE_METERS,
    private val nearbyPoiRadiusMeters: Double = NEARBY_POI_RADIUS_METERS,
    private val nearbyPoiLimit: Int = NEARBY_POI_LIMIT
) : SearchDockStore {

    private val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.main)
    private val mutableState = MutableStateFlow(SearchDockState())
    private val effectChannel = Channel<SearchDockEffect>(capacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private var latestLocation: Coordinate? = null
    private var lastNearbyPoiFetchLocation: Coordinate? = null
    private var averageGroundSpeedMetersPerSecond: Double? = null
    private var speedSampleCount: Int = 0

    override val state: StateFlow<SearchDockState> = mutableState.asStateFlow()
    override val effects: Flow<SearchDockEffect> = effectChannel.receiveAsFlow()

    override fun send(intent: SearchDockIntent) {
        when (intent) {
            is SearchDockIntent.UserLocationChanged -> {
                latestLocation = Coordinate(intent.latitude, intent.longitude)
                recordSpeedSample(intent.speedMetersPerSecond)
                reduce(intent)
                updateNavigationProgress()
                if (state.value.isExpanded && state.value.activeRoute == SearchDockRoute.Nearby) {
                    loadNearbyPoiIfNeeded(force = state.value.nearbyPoiItems.isEmpty())
                }
            }

            SearchDockIntent.SubmitSearch -> submitCurrentQuery()
            is SearchDockIntent.ExpandedChanged -> {
                reduce(intent)
                if (intent.expanded && state.value.activeRoute == SearchDockRoute.Nearby) {
                    loadNearbyPoiIfNeeded(force = state.value.nearbyPoiItems.isEmpty())
                }
            }

            is SearchDockIntent.RouteSelected -> {
                reduce(intent)
                if (state.value.activeRoute == SearchDockRoute.Nearby && state.value.isExpanded) {
                    loadNearbyPoiIfNeeded(force = state.value.nearbyPoiItems.isEmpty())
                }
            }

            is SearchDockIntent.RoutePlanningChanged,
            is SearchDockIntent.MapSelectionChanged,
            is SearchDockIntent.MapTapLookupChanged,
            is SearchDockIntent.SearchQueryChanged,
            SearchDockIntent.SearchCleared,
            SearchDockIntent.NearbyPoiLoadRequested,
            is SearchDockIntent.NearbyPoiLoaded,
            is SearchDockIntent.NearbyPoiFailed,
            is SearchDockIntent.SearchSucceeded,
            is SearchDockIntent.SearchFailed,
            is SearchDockIntent.OpenMapTapDetail,
            SearchDockIntent.CloseMapTapDetail,
            is SearchDockIntent.OpenNavigationWaypointDetail,
            SearchDockIntent.CloseNavigationDetail -> reduce(intent)

            is SearchDockIntent.RouteDestinationAdded,
            is SearchDockIntent.RouteDestinationRemoved,
            SearchDockIntent.OpenNavigationDetail,
            SearchDockIntent.AddWaypointRequested,
            SearchDockIntent.EndFlight -> {
                reduce(intent)
                if (intent == SearchDockIntent.EndFlight) {
                    resetNavigationProgress()
                } else {
                    updateNavigationProgress()
                }
            }

            is SearchDockIntent.NavigationProgressChanged -> reduce(intent)
        }
    }

    override fun close() {
        scope.cancel()
        effectChannel.close()
    }

    private fun submitCurrentQuery() {
        reduce(SearchDockIntent.SubmitSearch)
        val query = state.value.searchQuery
        scope.launch(dispatcherProvider.io) {
            when (val result = searchUseCase(query)) {
                is AppResult.Success -> {
                    reduce(SearchDockIntent.SearchSucceeded(result.value))
                }

                is AppResult.Failure -> {
                    reduce(SearchDockIntent.SearchFailed(result.error))
                    val validationFailure = result.error as? Failure.Validation
                    if (validationFailure != null) {
                        effectChannel.trySend(SearchDockEffect.ShowMessage(validationFailure.message))
                    }
                }
            }
        }
    }

    private fun loadNearbyPoiIfNeeded(force: Boolean) {
        val location = latestLocation ?: return
        if (!force && !shouldRefetchNearbyPoi(location)) {
            return
        }
        lastNearbyPoiFetchLocation = location
        reduce(SearchDockIntent.NearbyPoiLoadRequested)

        scope.launch(dispatcherProvider.io) {
            val airportsResult = queryNearbyAirportsUseCase(
                latitude = location.latitude,
                longitude = location.longitude,
                radiusMeters = nearbyPoiRadiusMeters,
                limit = nearbyPoiLimit
            )
            val navaidsResult = queryNearbyNavaidsUseCase(
                latitude = location.latitude,
                longitude = location.longitude,
                radiusMeters = nearbyPoiRadiusMeters,
                limit = nearbyPoiLimit
            )

            val mergedItems = mutableListOf<SearchDockPoiItem>()
            val airports = (airportsResult as? AppResult.Success)?.value.orEmpty()
            val navaids = (navaidsResult as? AppResult.Success)?.value.orEmpty()
            mergedItems += airports.map { it.toPoiItem() }
            mergedItems += navaids.map { it.toPoiItem() }

            val hasAnyData = mergedItems.isNotEmpty()
            if (!hasAnyData) {
                val failure = (airportsResult as? AppResult.Failure)?.error
                    ?: (navaidsResult as? AppResult.Failure)?.error
                    ?: Failure.DataUnavailable
                reduce(SearchDockIntent.NearbyPoiFailed(failure))
                return@launch
            }

            val sorted = mergedItems.sortedBy { it.distanceMeters }.take(nearbyPoiLimit * 2)
            reduce(SearchDockIntent.NearbyPoiLoaded(sorted))
        }
    }

    private fun updateNavigationProgress() {
        val currentState = state.value
        if (!currentState.isNavigating || currentState.routeDestinations.isEmpty()) {
            return
        }
        val location = latestLocation
        val remainingDistance = if (location != null) {
            remainingRouteDistanceMeters(location, currentState.routeDestinations)
        } else {
            routeDistanceMeters(currentState.routeDestinations)
        }
        val speed = averageGroundSpeedMetersPerSecond
            ?.takeIf { it >= MIN_NAVIGATION_SPEED_METERS_PER_SECOND }
            ?: DEFAULT_SMALL_AIRCRAFT_SPEED_METERS_PER_SECOND
        val remainingSeconds = (remainingDistance / speed).roundToInt().toLong()
        reduce(
            SearchDockIntent.NavigationProgressChanged(
                SearchDockNavigationSummary(
                    remainingDistanceMeters = remainingDistance,
                    remainingSeconds = remainingSeconds,
                    speedMetersPerSecond = speed
                )
            )
        )
    }

    private fun recordSpeedSample(speedMetersPerSecond: Double?) {
        val speed = speedMetersPerSecond?.takeIf { it >= MIN_NAVIGATION_SPEED_METERS_PER_SECOND } ?: return
        val previousAverage = averageGroundSpeedMetersPerSecond
        if (previousAverage == null) {
            averageGroundSpeedMetersPerSecond = speed
            speedSampleCount = 1
            return
        }
        speedSampleCount += 1
        averageGroundSpeedMetersPerSecond = previousAverage + (speed - previousAverage) / speedSampleCount
    }

    private fun resetNavigationProgress() {
        averageGroundSpeedMetersPerSecond = null
        speedSampleCount = 0
    }

    private fun remainingRouteDistanceMeters(current: Coordinate, destinations: List<SearchDockRoutePoint>): Double {
        if (destinations.isEmpty()) return 0.0
        var total = haversineMeters(
            lat1 = current.latitude,
            lon1 = current.longitude,
            lat2 = destinations.first().latitude,
            lon2 = destinations.first().longitude
        )
        total += routeDistanceMeters(destinations)
        return total
    }

    private fun routeDistanceMeters(destinations: List<SearchDockRoutePoint>): Double {
        if (destinations.size < 2) return 0.0
        return destinations.zipWithNext().sumOf { (from, to) ->
            haversineMeters(
                lat1 = from.latitude,
                lon1 = from.longitude,
                lat2 = to.latitude,
                lon2 = to.longitude
            )
        }
    }

    private fun shouldRefetchNearbyPoi(current: Coordinate): Boolean {
        val previous = lastNearbyPoiFetchLocation ?: return true
        return haversineMeters(
            lat1 = previous.latitude,
            lon1 = previous.longitude,
            lat2 = current.latitude,
            lon2 = current.longitude
        ) >= minNearbyPoiRefetchDistanceMeters
    }

    private fun reduce(intent: SearchDockIntent) {
        mutableState.value = reducer.reduce(mutableState.value, intent)
    }

    private fun NearbyAirport.toPoiItem(): SearchDockPoiItem {
        return SearchDockPoiItem(
            id = "airport:${airport.id}",
            title = airport.id,
            subtitle = airport.name,
            kindLabel = "Airport",
            distanceMeters = distanceMeters,
            latitude = airport.latitude,
            longitude = airport.longitude
        )
    }

    private fun NearbyNavaid.toPoiItem(): SearchDockPoiItem {
        return SearchDockPoiItem(
            id = "navaid:${navaid.id}",
            title = navaid.id,
            subtitle = "${navaid.name} · ${navaid.detail}",
            kindLabel = "Navaid",
            frequency = navaid.frequency,
            distanceMeters = distanceMeters,
            latitude = navaid.latitude,
            longitude = navaid.longitude
        )
    }

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = (lat2 - lat1).toRadians()
        val dLon = (lon2 - lon1).toRadians()
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(lat1.toRadians()) * cos(lat2.toRadians()) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    private fun Double.toRadians(): Double = this * PI / 180.0

    private data class Coordinate(
        val latitude: Double,
        val longitude: Double
    )

    companion object {
        const val EARTH_RADIUS_METERS = 6_371_000.0
        const val MIN_NEARBY_POI_REFETCH_DISTANCE_METERS = 150.0
        const val NEARBY_POI_RADIUS_METERS = 25_000.0
        const val NEARBY_POI_LIMIT = 6
        const val DEFAULT_SMALL_AIRCRAFT_SPEED_METERS_PER_SECOND = 51.4
        const val MIN_NAVIGATION_SPEED_METERS_PER_SECOND = 5.0

        fun formatDistance(distanceMeters: Double): String {
            return if (distanceMeters >= 1000.0) {
                "${(distanceMeters / 1000.0 * 10.0).roundToInt() / 10.0} km"
            } else {
                "${distanceMeters.roundToInt()} m"
            }
        }
    }
}
