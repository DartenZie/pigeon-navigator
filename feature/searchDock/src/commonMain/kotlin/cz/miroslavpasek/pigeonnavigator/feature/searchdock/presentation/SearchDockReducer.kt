package cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation

import cz.miroslavpasek.pigeonnavigator.domain.failure.Failure

class SearchDockReducer(
    private val routeResolver: SearchDockRouteResolver
) {
    fun reduce(state: SearchDockState, intent: SearchDockIntent): SearchDockState {
        val next = when (intent) {
            is SearchDockIntent.ExpandedChanged -> state.copy(isExpanded = intent.expanded)
            is SearchDockIntent.RouteSelected -> state.copy(selectedRouteOverride = intent.route)
            is SearchDockIntent.RoutePlanningChanged -> state.copy(isRoutePlanning = intent.planning)
            is SearchDockIntent.MapSelectionChanged -> state.copy(hasMapSelection = intent.hasSelection)
            is SearchDockIntent.UserLocationChanged -> state
            is SearchDockIntent.SearchQueryChanged -> state.copy(
                searchQuery = intent.query,
                searchErrorMessage = null,
                searchResults = if (intent.query.isBlank()) emptyList() else state.searchResults,
                selectedRouteOverride = if (intent.query.isBlank()) null else state.selectedRouteOverride
            )

            SearchDockIntent.SubmitSearch -> state.copy(
                isExpanded = true,
                isSearching = true,
                searchErrorMessage = null,
                selectedRouteOverride = null
            )

            SearchDockIntent.SearchCleared -> state.copy(
                searchQuery = "",
                isSearching = false,
                searchResults = emptyList(),
                searchErrorMessage = null,
                selectedRouteOverride = null
            )

            is SearchDockIntent.SearchSucceeded -> state.copy(
                isSearching = false,
                searchResults = intent.results,
                searchErrorMessage = null,
                selectedRouteOverride = null
            )

            is SearchDockIntent.SearchFailed -> state.copy(
                isSearching = false,
                searchErrorMessage = intent.failure.toMessage(),
                selectedRouteOverride = null
            )

            SearchDockIntent.NearbyPoiLoadRequested -> state.copy(
                isNearbyPoiLoading = true,
                nearbyPoiErrorMessage = null
            )

            is SearchDockIntent.NearbyPoiLoaded -> state.copy(
                isNearbyPoiLoading = false,
                nearbyPoiErrorMessage = null,
                nearbyPoiItems = intent.items
            )

            is SearchDockIntent.NearbyPoiFailed -> state.copy(
                isNearbyPoiLoading = false,
                nearbyPoiErrorMessage = intent.failure.toMessage()
            )
        }

        val availableRoutes = routeResolver.availableRoutes(next)
        val overrideRoute = next.selectedRouteOverride?.takeIf { availableRoutes.contains(it) }
        val activeRoute = overrideRoute ?: routeResolver.resolveDefaultRoute(next, availableRoutes)

        return next.copy(
            selectedRouteOverride = overrideRoute,
            availableRoutes = availableRoutes,
            activeRoute = activeRoute
        )
    }

    private fun Failure.toMessage(): String = when (this) {
        Failure.OutOfCoverage,
        Failure.DataUnavailable -> "Data unavailable"

        is Failure.Validation -> message
        Failure.Unexpected -> "Unexpected error"
    }
}
