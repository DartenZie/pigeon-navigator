package cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation

import kotlin.test.Test
import kotlin.test.assertEquals

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
    fun mapTapRouteAppearsWhenSelectionExists() {
        val next = reducer.reduce(SearchDockState(), SearchDockIntent.MapSelectionChanged(hasSelection = true))

        assertEquals(SearchDockRoute.MapPointDetails, next.activeRoute)
        assertEquals(true, next.availableRoutes.contains(SearchDockRoute.MapPointDetails))
    }
}
