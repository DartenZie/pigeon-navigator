package cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation

class SearchDockRouteResolver {
    fun availableRoutes(state: SearchDockState): List<SearchDockRoute> {
        val routes = mutableListOf(SearchDockRoute.NearbyPoi)
        if (state.searchQuery.isNotBlank() || state.searchResults.isNotEmpty()) {
            routes += SearchDockRoute.SearchResults
        }
        if (state.hasMapSelection) {
            routes += SearchDockRoute.MapPointDetails
        }
        if (state.isRoutePlanning) {
            routes += SearchDockRoute.RoutePlanner
        }
        return routes
    }

    fun resolveDefaultRoute(
        state: SearchDockState,
        availableRoutes: List<SearchDockRoute>
    ): SearchDockRoute {
        return when {
            state.isRoutePlanning && availableRoutes.contains(SearchDockRoute.RoutePlanner) -> SearchDockRoute.RoutePlanner
            state.searchQuery.isNotBlank() && availableRoutes.contains(SearchDockRoute.SearchResults) -> SearchDockRoute.SearchResults
            state.searchResults.isNotEmpty() && availableRoutes.contains(SearchDockRoute.SearchResults) -> SearchDockRoute.SearchResults
            state.hasMapSelection && availableRoutes.contains(SearchDockRoute.MapPointDetails) -> SearchDockRoute.MapPointDetails
            else -> SearchDockRoute.NearbyPoi
        }
    }
}
