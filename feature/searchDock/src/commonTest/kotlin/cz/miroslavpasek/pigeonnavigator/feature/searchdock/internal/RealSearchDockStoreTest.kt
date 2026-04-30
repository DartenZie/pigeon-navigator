package cz.miroslavpasek.pigeonnavigator.feature.searchdock.internal

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
import cz.miroslavpasek.pigeonnavigator.domain.aviation.QueryNearbyAirportsUseCase
import cz.miroslavpasek.pigeonnavigator.domain.aviation.QueryNearbyNavaidsUseCase
import cz.miroslavpasek.pigeonnavigator.domain.failure.Failure
import cz.miroslavpasek.pigeonnavigator.domain.search.SearchRepository
import cz.miroslavpasek.pigeonnavigator.domain.search.SearchResult
import cz.miroslavpasek.pigeonnavigator.domain.search.SearchUseCase
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockIntent
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockReducer
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockRoute
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockRoutePoint
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockRouteResolver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RealSearchDockStoreTest {

    @Test
    fun submitSearchPublishesResultsAndSearchRoute() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val store = createStore(
            dispatcher = dispatcher,
            searchRepository = FakeSearchRepository(
                results = listOf(
                    SearchResult.Airport(
                        id = "airport:LKPR",
                        title = "LKPR",
                        subtitle = "Prague",
                        latitude = 50.1008,
                        longitude = 14.26
                    ),
                    SearchResult.Navaid(
                        id = "navaid:PRG",
                        title = "PRG",
                        subtitle = "Prague VOR · VOR",
                        frequency = "115.9",
                        latitude = 50.0,
                        longitude = 14.0
                    )
                ),
                shouldFail = false
            )
        )

        store.send(SearchDockIntent.ExpandedChanged(expanded = true))
        store.send(SearchDockIntent.SearchQueryChanged("pr"))
        store.send(SearchDockIntent.SubmitSearch)
        advanceUntilIdle()

        val state = store.state.value
        assertEquals(SearchDockRoute.Search, state.activeRoute)
        assertEquals(listOf("LKPR", "PRG"), state.searchResults.map { it.title })
        assertEquals(false, state.isSearching)

        store.close()
    }

    @Test
    fun blankSearchSetsValidationError() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val store = createStore(
            dispatcher = dispatcher,
            searchRepository = FakeSearchRepository(results = emptyList(), shouldFail = false)
        )

        store.send(SearchDockIntent.ExpandedChanged(expanded = true))
        store.send(SearchDockIntent.SearchQueryChanged(""))
        store.send(SearchDockIntent.SubmitSearch)
        advanceUntilIdle()

        val state = store.state.value
        assertEquals("Query cannot be blank", state.searchErrorMessage)
        assertEquals(false, state.isSearching)
        assertTrue(state.availableRoutes.contains(SearchDockRoute.Nearby))

        store.close()
    }

    @Test
    fun routeDestinationStartsNavigationAndLocationUpdatesProgress() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val store = createStore(
            dispatcher = dispatcher,
            searchRepository = FakeSearchRepository(results = emptyList(), shouldFail = false)
        )

        store.send(
            SearchDockIntent.RouteDestinationAdded(
                SearchDockRoutePoint(
                    id = "airport:LKPR",
                    title = "LKPR",
                    latitude = 50.0,
                    longitude = 14.1
                )
            )
        )
        store.send(
            SearchDockIntent.UserLocationChanged(
                latitude = 50.0,
                longitude = 14.0,
                speedMetersPerSecond = 40.0
            )
        )

        val state = store.state.value
        assertEquals(SearchDockRoute.NavigationDetail, state.activeRoute)
        assertTrue(state.isNavigating)
        assertTrue((state.navigationSummary?.remainingDistanceMeters ?: 0.0) > 0.0)
        assertTrue((state.navigationSummary?.remainingSeconds ?: 0L) > 0L)

        store.close()
    }

    private fun createStore(
        dispatcher: CoroutineDispatcher,
        searchRepository: SearchRepository
    ): RealSearchDockStore {
        val aviationRepository = FakeAviationRepository()
        return RealSearchDockStore(
            searchUseCase = SearchUseCase(searchRepository),
            queryNearbyAirportsUseCase = QueryNearbyAirportsUseCase(repository = aviationRepository),
            queryNearbyNavaidsUseCase = QueryNearbyNavaidsUseCase(repository = aviationRepository),
            reducer = SearchDockReducer(routeResolver = SearchDockRouteResolver()),
            dispatcherProvider = TestDispatcherProvider(dispatcher)
        )
    }
}

private class TestDispatcherProvider(
    dispatcher: CoroutineDispatcher
) : DispatcherProvider {
    override val main: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
    override val default: CoroutineDispatcher = dispatcher
}

private class FakeSearchRepository(
    private val results: List<SearchResult>,
    private val shouldFail: Boolean
) : SearchRepository {
    override suspend fun search(query: String): AppResult<List<SearchResult>, Failure> {
        if (shouldFail) {
            return AppResult.Failure(Failure.DataUnavailable)
        }
        return AppResult.Success(results)
    }
}

private class FakeAviationRepository : AviationRepository {
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
        return AppResult.Success(
            listOf(
                NearbyAirport(
                    airport = Airport(
                        id = "LKPR",
                        name = "Vaclav Havel",
                        kind = "large_airport",
                        latitude = latitude,
                        longitude = longitude,
                        elevationMeters = null
                    ),
                    distanceMeters = 1500.0
                )
            )
        )
    }

    override suspend fun nearbyNavaids(
        latitude: Double,
        longitude: Double,
        radiusMeters: Double,
        limit: Int
    ): AppResult<List<NearbyNavaid>, Failure> {
        return AppResult.Success(
            listOf(
                NearbyNavaid(
                    navaid = Navaid(
                        id = "PRG",
                        name = "Prague VOR",
                        kind = "VOR",
                        detail = "115.9",
                        frequency = "115.9",
                        latitude = latitude,
                        longitude = longitude
                    ),
                    distanceMeters = 2200.0
                )
            )
        )
    }

    override suspend fun activePackageFilesExist(): Boolean = true

    override suspend fun containingAirspaces(
        latitude: Double,
        longitude: Double
    ): AppResult<List<Airspace>, Failure> {
        return AppResult.Success(emptyList())
    }
}
