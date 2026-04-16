package cz.miroslavpasek.pigeonnavigator.bridge

import cz.miroslavpasek.pigeonnavigator.core.platform.coroutines.DispatcherProvider
import cz.miroslavpasek.pigeonnavigator.core.util.result.AppResult
import cz.miroslavpasek.pigeonnavigator.domain.aviation.Airspace
import cz.miroslavpasek.pigeonnavigator.domain.aviation.NearbyAirport
import cz.miroslavpasek.pigeonnavigator.domain.aviation.QueryContainingAirspacesUseCase
import cz.miroslavpasek.pigeonnavigator.domain.aviation.QueryNearbyAirportsUseCase
import cz.miroslavpasek.pigeonnavigator.domain.failure.Failure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
    val airspaces: List<Airspace> = emptyList()
)

class MapTapLookupCoordinator(
    private val queryNearbyAirportsUseCase: QueryNearbyAirportsUseCase,
    private val queryContainingAirspacesUseCase: QueryContainingAirspacesUseCase,
    private val dispatcherProvider: DispatcherProvider,
    private val minRefetchDistanceMeters: Double = MIN_REFETCH_DISTANCE_METERS,
    private val airportTapRadiusMeters: Double = AIRPORT_TAP_RADIUS_METERS,
    private val airportTapLimit: Int = AIRPORT_TAP_LIMIT
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
                errorMessage = null
            )
        }

        scope.launch(dispatcherProvider.io) {
            val airportsResult = queryNearbyAirportsUseCase(
                latitude = latitude,
                longitude = longitude,
                radiusMeters = airportTapRadiusMeters,
                limit = airportTapLimit
            )
            val airspacesResult = queryContainingAirspacesUseCase(latitude = latitude, longitude = longitude)

            mutableState.update { current ->
                current.copy(
                    isLoading = false,
                    errorMessage = resolveError(airportsResult, airspacesResult),
                    airports = (airportsResult as? AppResult.Success)?.value ?: emptyList(),
                    airspaces = (airspacesResult as? AppResult.Success)?.value ?: emptyList()
                )
            }
        }
    }

    fun close() {
        scope.cancel()
    }

    private fun resolveError(
        airportsResult: AppResult<List<NearbyAirport>, Failure>,
        airspacesResult: AppResult<List<Airspace>, Failure>
    ): String? {
        val airportFailure = airportsResult as? AppResult.Failure
        val airspaceFailure = airspacesResult as? AppResult.Failure

        return when {
            airportFailure == null && airspaceFailure == null -> null
            airportFailure != null && airspaceFailure != null -> "Airspace and airport data unavailable"
            airportFailure != null -> "Airport data unavailable"
            else -> "Airspace data unavailable"
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
    }
}
