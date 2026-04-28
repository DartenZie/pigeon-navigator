package cz.miroslavpasek.pigeonnavigator.bridge

import cz.miroslavpasek.pigeonnavigator.core.platform.coroutines.DispatcherProvider
import cz.miroslavpasek.pigeonnavigator.core.util.result.AppResult
import cz.miroslavpasek.pigeonnavigator.domain.aviation.ActiveMapPackage
import cz.miroslavpasek.pigeonnavigator.domain.aviation.Airport
import cz.miroslavpasek.pigeonnavigator.domain.aviation.Airspace
import cz.miroslavpasek.pigeonnavigator.domain.aviation.AviationPackageSource
import cz.miroslavpasek.pigeonnavigator.domain.aviation.AviationRepository
import cz.miroslavpasek.pigeonnavigator.domain.aviation.Navaid
import cz.miroslavpasek.pigeonnavigator.domain.aviation.NearbyAirport
import cz.miroslavpasek.pigeonnavigator.domain.aviation.NearbyNavaid
import cz.miroslavpasek.pigeonnavigator.domain.aviation.QueryContainingAirspacesUseCase
import cz.miroslavpasek.pigeonnavigator.domain.aviation.QueryNearbyAirportsUseCase
import cz.miroslavpasek.pigeonnavigator.domain.aviation.QueryNearbyNavaidsUseCase
import cz.miroslavpasek.pigeonnavigator.domain.failure.Failure
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MapTapLookupCoordinatorTest {

    @Test
    fun queryAtUsesTapRadiiAndSingleResultLimitsForAirportsAndNavaids() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val repository = RecordingAviationRepository()
        val coordinator = MapTapLookupCoordinator(
            queryNearbyAirportsUseCase = QueryNearbyAirportsUseCase(repository = repository),
            queryContainingAirspacesUseCase = QueryContainingAirspacesUseCase(repository = repository),
            queryNearbyNavaidsUseCase = QueryNearbyNavaidsUseCase(repository = repository),
            dispatcherProvider = TestDispatcherProvider(dispatcher)
        )

        coordinator.queryAt(latitude = 50.10, longitude = 14.42)
        advanceUntilIdle()

        val airportQuery = repository.lastNearbyAirportQuery
        assertEquals(1000.0, airportQuery?.radiusMeters)
        assertEquals(1, airportQuery?.limit)
        assertEquals(1, coordinator.state.value.airports.size)

        val navaidQuery = repository.lastNearbyNavaidQuery
        assertEquals(1000.0, navaidQuery?.radiusMeters)
        assertEquals(1, navaidQuery?.limit)
        assertEquals(1, coordinator.state.value.navaids.size)

        coordinator.close()
    }

    @Test
    fun lookupSequenceIncrementsForFreshTapAndStaysSameForSuppressedRefetch() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val repository = RecordingAviationRepository()
        val coordinator = MapTapLookupCoordinator(
            queryNearbyAirportsUseCase = QueryNearbyAirportsUseCase(repository = repository),
            queryContainingAirspacesUseCase = QueryContainingAirspacesUseCase(repository = repository),
            queryNearbyNavaidsUseCase = QueryNearbyNavaidsUseCase(repository = repository),
            dispatcherProvider = TestDispatcherProvider(dispatcher)
        )

        assertEquals(0L, coordinator.state.value.lookupSequence)

        coordinator.queryAt(latitude = 50.10, longitude = 14.42)
        advanceUntilIdle()
        val firstSequence = coordinator.state.value.lookupSequence
        assertTrue(firstSequence > 0L)

        // Same point: refetch suppressed, sequence must NOT advance.
        coordinator.queryAt(latitude = 50.10, longitude = 14.42)
        advanceUntilIdle()
        assertEquals(firstSequence, coordinator.state.value.lookupSequence)

        // Far point: must produce a new fetch and a new sequence.
        coordinator.queryAt(latitude = 51.20, longitude = 15.50)
        advanceUntilIdle()
        assertTrue(coordinator.state.value.lookupSequence > firstSequence)

        coordinator.close()
    }
}

private class TestDispatcherProvider(
    dispatcher: CoroutineDispatcher
) : DispatcherProvider {
    override val main: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
    override val default: CoroutineDispatcher = dispatcher
}

private class RecordingAviationRepository : AviationRepository {
    data class NearbyAirportQuery(
        val latitude: Double,
        val longitude: Double,
        val radiusMeters: Double,
        val limit: Int
    )

    data class NearbyNavaidQuery(
        val latitude: Double,
        val longitude: Double,
        val radiusMeters: Double,
        val limit: Int
    )

    var lastNearbyAirportQuery: NearbyAirportQuery? = null
    var lastNearbyNavaidQuery: NearbyNavaidQuery? = null

    override suspend fun installPackage(source: AviationPackageSource): AppResult<Unit, Failure> {
        return AppResult.Success(Unit)
    }

    override suspend fun getActiveMapPackage(): AppResult<ActiveMapPackage, Failure> {
        return AppResult.Success(
            ActiveMapPackage(
                packageId = "pkg",
                mapPmtilesAbsolutePath = "/tmp/map.pmtiles",
                terrainPmtilesAbsolutePath = "/tmp/terrain.pmtiles"
            )
        )
    }

    override suspend fun nearbyAirports(
        latitude: Double,
        longitude: Double,
        radiusMeters: Double,
        limit: Int
    ): AppResult<List<NearbyAirport>, Failure> {
        lastNearbyAirportQuery = NearbyAirportQuery(
            latitude = latitude,
            longitude = longitude,
            radiusMeters = radiusMeters,
            limit = limit
        )

        val airports = listOf(
            NearbyAirport(
                airport = Airport(
                    id = "LKAA",
                    name = "A",
                    kind = "small_airport",
                    latitude = latitude,
                    longitude = longitude,
                    elevationMeters = null
                ),
                distanceMeters = 10.0
            ),
            NearbyAirport(
                airport = Airport(
                    id = "LKBB",
                    name = "B",
                    kind = "small_airport",
                    latitude = latitude,
                    longitude = longitude,
                    elevationMeters = null
                ),
                distanceMeters = 20.0
            )
        )

        return AppResult.Success(airports.take(limit))
    }

    override suspend fun nearbyNavaids(
        latitude: Double,
        longitude: Double,
        radiusMeters: Double,
        limit: Int
    ): AppResult<List<NearbyNavaid>, Failure> {
        lastNearbyNavaidQuery = NearbyNavaidQuery(
            latitude = latitude,
            longitude = longitude,
            radiusMeters = radiusMeters,
            limit = limit
        )

        val navaids = listOf(
            NearbyNavaid(
                navaid = Navaid(
                    id = "PRG",
                    name = "Prague",
                    kind = "VOR",
                    detail = "114.30",
                    frequency = "114.30",
                    latitude = latitude,
                    longitude = longitude
                ),
                distanceMeters = 5.0
            ),
            NearbyNavaid(
                navaid = Navaid(
                    id = "OKL",
                    name = "Okruh",
                    kind = "NDB",
                    detail = "350",
                    frequency = "350",
                    latitude = latitude,
                    longitude = longitude
                ),
                distanceMeters = 15.0
            )
        )

        return AppResult.Success(navaids.take(limit))
    }

    override suspend fun activePackageFilesExist(): Boolean = true

    override suspend fun containingAirspaces(
        latitude: Double,
        longitude: Double
    ): AppResult<List<Airspace>, Failure> {
        return AppResult.Success(emptyList())
    }
}
