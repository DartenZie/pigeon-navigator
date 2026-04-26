package cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation

import cz.miroslavpasek.pigeonnavigator.domain.search.SearchResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchDockReducerTest {

    private val reducer = SearchDockReducer(routeResolver = SearchDockRouteResolver())

    @Test
    fun routePlanningWinsOverOtherRoutes() {
        val state = SearchDockState(
            hasMapSelection = true,
            searchQuery = "prg"
        )

        val next = reducer.reduce(state, SearchDockIntent.RoutePlanningChanged(planning = true))

        assertEquals(SearchDockRoute.RoutePlanner, next.activeRoute)
    }

    @Test
    fun mapTapRouteAlwaysAvailable() {
        val next = reducer.reduce(SearchDockState(), SearchDockIntent.MapSelectionChanged(hasSelection = true))

        assertTrue(next.availableRoutes.contains(SearchDockRoute.MapTap))
    }

    @Test
    fun mapSelectionDoesNotOverrideDefaultRoute() {
        val next = reducer.reduce(SearchDockState(), SearchDockIntent.MapSelectionChanged(hasSelection = true))

        assertEquals(SearchDockRoute.Nearby, next.activeRoute)
    }

    @Test
    fun searchQuerySelectsSearchRoute() {
        val next = reducer.reduce(SearchDockState(), SearchDockIntent.SearchQueryChanged("prg"))

        assertEquals(SearchDockRoute.Search, next.activeRoute)
    }

    @Test
    fun expandingWithoutSpecialStateDefaultsToNearby() {
        val state = SearchDockState(
            isExpanded = false,
            activeRoute = SearchDockRoute.Search,
            selectedRouteOverride = SearchDockRoute.Search
        )

        val next = reducer.reduce(state, SearchDockIntent.ExpandedChanged(expanded = true))

        assertEquals(SearchDockRoute.Nearby, next.activeRoute)
        assertEquals(null, next.selectedRouteOverride)
    }

    @Test
    fun collapsingClearsSearchInputAndResults() {
        val state = SearchDockState(
            isExpanded = true,
            searchQuery = "lkpr",
            isSearching = true,
            searchResults = listOf(
                SearchResult.Airport(
                    id = "airport:LKPR",
                    title = "LKPR",
                    subtitle = "Prague",
                    latitude = 50.1008,
                    longitude = 14.26
                )
            ),
            searchErrorMessage = "Error"
        )

        val next = reducer.reduce(state, SearchDockIntent.ExpandedChanged(expanded = false))

        assertEquals("", next.searchQuery)
        assertEquals(false, next.isSearching)
        assertEquals(emptyList(), next.searchResults)
        assertEquals(null, next.searchErrorMessage)
    }
}
