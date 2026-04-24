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
            state.isRoutePlanning -> SearchDockRoute.RoutePlanner
            state.searchQuery.isNotBlank() -> SearchDockRoute.Search
            state.searchResults.isNotEmpty() -> SearchDockRoute.Search
            else -> SearchDockRoute.Nearby
        }
    }
}
