package cz.miroslavpasek.pigeonnavigator.bridge

import cz.miroslavpasek.pigeonnavigator.core.platform.coroutines.DispatcherProvider
import cz.miroslavpasek.pigeonnavigator.core.util.result.AppResult
import cz.miroslavpasek.pigeonnavigator.domain.aviation.Airspace
import cz.miroslavpasek.pigeonnavigator.domain.aviation.NearbyAirport
import cz.miroslavpasek.pigeonnavigator.domain.aviation.NearbyNavaid
import cz.miroslavpasek.pigeonnavigator.domain.aviation.QueryContainingAirspacesUseCase
import cz.miroslavpasek.pigeonnavigator.domain.aviation.QueryNearbyAirportsUseCase
import cz.miroslavpasek.pigeonnavigator.domain.aviation.QueryNearbyNavaidsUseCase
import cz.miroslavpasek.pigeonnavigator.domain.failure.Failure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class MapTapLookupState(
    val selectedLatitude: Double? = null,
    val selectedLongitude: Double? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val airports: List<NearbyAirport> = emptyList(),
    val airspaces: List<Airspace> = emptyList(),
    val navaids: List<NearbyNavaid> = emptyList(),
    /**
     * Monotonically-increasing cursor that increments on every fetch actually
     * launched by [MapTapLookupCoordinator.queryAt] (i.e. excluding refetch
     * skips). Downstream consumers use this to detect a fresh result and
     * avoid acting on the same lookup twice.
     */
    val lookupSequence: Long = 0L
)

class MapTapLookupCoordinator(
    private val queryNearbyAirportsUseCase: QueryNearbyAirportsUseCase,
    private val queryContainingAirspacesUseCase: QueryContainingAirspacesUseCase,
    private val queryNearbyNavaidsUseCase: QueryNearbyNavaidsUseCase,
    private val dispatcherProvider: DispatcherProvider,
    private val minRefetchDistanceMeters: Double = MIN_REFETCH_DISTANCE_METERS,
    private val airportTapRadiusMeters: Double = AIRPORT_TAP_RADIUS_METERS,
    private val airportTapLimit: Int = AIRPORT_TAP_LIMIT,
    private val navaidTapRadiusMeters: Double = NAVAID_TAP_RADIUS_METERS,
    private val navaidTapLimit: Int = NAVAID_TAP_LIMIT
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.main)
    private val mutableState = MutableStateFlow(MapTapLookupState())
    private var lastFetchedLatitude: Double? = null
    private var lastFetchedLongitude: Double? = null

    val state: StateFlow<MapTapLookupState> = mutableState.asStateFlow()

    fun queryAt(latitude: Double, longitude: Double) {
        mutableState.update {
            it.copy(
                selectedLatitude = latitude,
                selectedLongitude = longitude
            )
        }

        val previousLatitude = lastFetchedLatitude
        val previousLongitude = lastFetchedLongitude
        if (
            previousLatitude != null && previousLongitude != null &&
            haversineMeters(previousLatitude, previousLongitude, latitude, longitude) < minRefetchDistanceMeters
        ) {
            return
        }

        lastFetchedLatitude = latitude
        lastFetchedLongitude = longitude

        mutableState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                lookupSequence = it.lookupSequence + 1L
            )
        }

        scope.launch(dispatcherProvider.io) {
            val (airportsResult, airspacesResult, navaidsResult) = coroutineScope {
                val airportsDeferred = async {
                    queryNearbyAirportsUseCase(
                        latitude = latitude,
                        longitude = longitude,
                        radiusMeters = airportTapRadiusMeters,
                        limit = airportTapLimit
                    )
                }
                val airspacesDeferred = async {
                    queryContainingAirspacesUseCase(latitude = latitude, longitude = longitude)
                }
                val navaidsDeferred = async {
                    queryNearbyNavaidsUseCase(
                        latitude = latitude,
                        longitude = longitude,
                        radiusMeters = navaidTapRadiusMeters,
                        limit = navaidTapLimit
                    )
                }
                Triple(airportsDeferred.await(), airspacesDeferred.await(), navaidsDeferred.await())
            }

            mutableState.update { current ->
                current.copy(
                    isLoading = false,
                    errorMessage = resolveError(airportsResult, airspacesResult, navaidsResult),
                    airports = (airportsResult as? AppResult.Success)?.value ?: emptyList(),
                    airspaces = (airspacesResult as? AppResult.Success)?.value ?: emptyList(),
                    navaids = (navaidsResult as? AppResult.Success)?.value ?: emptyList()
                )
            }
        }
    }

    fun close() {
        scope.cancel()
    }

    private fun resolveError(
        airportsResult: AppResult<List<NearbyAirport>, Failure>,
        airspacesResult: AppResult<List<Airspace>, Failure>,
        navaidsResult: AppResult<List<NearbyNavaid>, Failure>
    ): String? {
        val airportFailure = airportsResult is AppResult.Failure
        val airspaceFailure = airspacesResult is AppResult.Failure
        val navaidFailure = navaidsResult is AppResult.Failure
        val failureCount = listOf(airportFailure, airspaceFailure, navaidFailure).count { it }

        return when {
            failureCount == 0 -> null
            failureCount >= 2 -> "Aviation data unavailable"
            airportFailure -> "Airport data unavailable"
            airspaceFailure -> "Airspace data unavailable"
            else -> "Navaid data unavailable"
        }
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

    private companion object {
        const val EARTH_RADIUS_METERS = 6_371_000.0
        const val MIN_REFETCH_DISTANCE_METERS = 120.0
        const val AIRPORT_TAP_RADIUS_METERS = 1000.0
        const val AIRPORT_TAP_LIMIT = 1
        const val NAVAID_TAP_RADIUS_METERS = 1000.0
        const val NAVAID_TAP_LIMIT = 1
    }
}
