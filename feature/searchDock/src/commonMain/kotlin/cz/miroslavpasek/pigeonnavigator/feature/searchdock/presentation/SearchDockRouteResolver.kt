package cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation

class SearchDockRouteResolver {
    fun availableRoutes(state: SearchDockState): List<SearchDockRoute> {
        return SearchDockRoute.entries
    }

    fun resolveDefaultRoute(
        state: SearchDockState,
        availableRoutes: List<SearchDockRoute>
    ): SearchDockRoute {
        return when {
            state.searchQuery.isNotBlank() -> SearchDockRoute.Search
            state.searchResults.isNotEmpty() -> SearchDockRoute.Search
            state.isRoutePlanning -> SearchDockRoute.RoutePlanner
            state.isNavigating -> SearchDockRoute.NavigationDetail
            else -> SearchDockRoute.Nearby
        }
    }
}
