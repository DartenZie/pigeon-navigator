package cz.miroslavpasek.pigeonnavigator.bridge

import cz.miroslavpasek.pigeonnavigator.core.platform.coroutines.DispatcherProvider
import cz.miroslavpasek.pigeonnavigator.core.util.result.AppResult
import cz.miroslavpasek.pigeonnavigator.domain.aviation.ActiveMapPackage
import cz.miroslavpasek.pigeonnavigator.domain.aviation.Airport
import cz.miroslavpasek.pigeonnavigator.domain.aviation.Airspace
import cz.miroslavpasek.pigeonnavigator.domain.aviation.AviationPackageSource
import cz.miroslavpasek.pigeonnavigator.domain.aviation.AviationRepository
import cz.miroslavpasek.pigeonnavigator.domain.aviation.NearbyAirport
import cz.miroslavpasek.pigeonnavigator.domain.aviation.NearbyNavaid
import cz.miroslavpasek.pigeonnavigator.domain.aviation.QueryContainingAirspacesUseCase
import cz.miroslavpasek.pigeonnavigator.domain.aviation.QueryNearbyAirportsUseCase
import cz.miroslavpasek.pigeonnavigator.domain.failure.Failure
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class MapTapLookupCoordinatorTest {

    @Test
    fun queryAtUsesTapAirportRadiusAndSingleResultLimit() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val repository = RecordingAviationRepository()
        val coordinator = MapTapLookupCoordinator(
            queryNearbyAirportsUseCase = QueryNearbyAirportsUseCase(repository = repository),
            queryContainingAirspacesUseCase = QueryContainingAirspacesUseCase(repository = repository),
            dispatcherProvider = TestDispatcherProvider(dispatcher)
        )

        coordinator.queryAt(latitude = 50.10, longitude = 14.42)
        advanceUntilIdle()

        val airportQuery = repository.lastNearbyAirportQuery
        assertEquals(250.0, airportQuery?.radiusMeters)
        assertEquals(1, airportQuery?.limit)
        assertEquals(1, coordinator.state.value.airports.size)

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

    var lastNearbyAirportQuery: NearbyAirportQuery? = null

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
        return AppResult.Success(emptyList())
    }

    override suspend fun activePackageFilesExist(): Boolean = true

    override suspend fun containingAirspaces(
        latitude: Double,
        longitude: Double
    ): AppResult<List<Airspace>, Failure> {
        return AppResult.Success(emptyList())
    }
}
