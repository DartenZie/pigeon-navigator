package cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation

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
}
