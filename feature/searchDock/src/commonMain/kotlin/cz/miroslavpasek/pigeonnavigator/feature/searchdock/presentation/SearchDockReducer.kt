package cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation

import cz.miroslavpasek.pigeonnavigator.domain.failure.Failure

class SearchDockReducer(
    private val routeResolver: SearchDockRouteResolver
) {
    fun reduce(state: SearchDockState, intent: SearchDockIntent): SearchDockState {
        val next = when (intent) {
            is SearchDockIntent.ExpandedChanged -> state.copy(
                isExpanded = intent.expanded,
                searchQuery = if (intent.expanded) state.searchQuery else "",
                isSearching = if (intent.expanded) state.isSearching else false,
                searchResults = if (intent.expanded) state.searchResults else emptyList(),
                searchErrorMessage = if (intent.expanded) state.searchErrorMessage else null,
                isRoutePlanning = if (!intent.expanded && state.isNavigating) false else state.isRoutePlanning,
                selectedRouteOverride = if (intent.expanded && state.hasNoSpecialState()) {
                    null
                } else if (!intent.expanded && state.isNavigating) {
                    null
                } else {
                    state.selectedRouteOverride
                },
                // Collapsing the dock dismisses any open detail panel so the
                // user comes back to the list view next time they expand.
                selectedMapTapDetailKey = if (intent.expanded) state.selectedMapTapDetailKey else null
            )
            is SearchDockIntent.RouteSelected -> state.copy(selectedRouteOverride = intent.route)
            is SearchDockIntent.RoutePlanningChanged -> state.copy(isRoutePlanning = intent.planning)
            is SearchDockIntent.MapSelectionChanged -> state.copy(hasMapSelection = intent.hasSelection)
            is SearchDockIntent.MapTapLookupChanged -> reduceMapTapLookupChanged(state, intent)
            is SearchDockIntent.UserLocationChanged -> state
            is SearchDockIntent.RouteDestinationAdded -> state.copy(
                isExpanded = true,
                isRoutePlanning = false,
                isNavigating = true,
                selectedRouteOverride = SearchDockRoute.NavigationDetail,
                selectedNavigationWaypointId = null,
                routeDestinations = state.routeDestinations + intent.point
            )
            is SearchDockIntent.RouteDestinationRemoved -> state.copy(
                routeDestinations = state.routeDestinations.filterNot { it.id == intent.id }
            ).let { next ->
                if (next.routeDestinations.isEmpty() && next.isNavigating) {
                    next.copy(
                        isNavigating = false,
                        isRoutePlanning = false,
                        navigationSummary = null,
                        selectedNavigationWaypointId = null,
                        selectedRouteOverride = null
                    )
                } else {
                    next
                }
            }
            SearchDockIntent.OpenNavigationDetail -> state.copy(
                isExpanded = true,
                isRoutePlanning = false,
                selectedNavigationWaypointId = null,
                selectedRouteOverride = SearchDockRoute.NavigationDetail
            )
            is SearchDockIntent.OpenNavigationWaypointDetail -> state.copy(
                isExpanded = true,
                isRoutePlanning = false,
                selectedNavigationWaypointId = intent.id,
                selectedRouteOverride = SearchDockRoute.NavigationDetail
            )
            SearchDockIntent.CloseNavigationDetail -> state.copy(selectedNavigationWaypointId = null)
            SearchDockIntent.AddWaypointRequested -> state.copy(
                isExpanded = true,
                isRoutePlanning = true,
                selectedNavigationWaypointId = null,
                selectedRouteOverride = SearchDockRoute.RoutePlanner
            )
            SearchDockIntent.EndFlight -> state.copy(
                isExpanded = false,
                isRoutePlanning = false,
                isNavigating = false,
                selectedRouteOverride = null,
                routeDestinations = emptyList(),
                navigationSummary = null,
                selectedNavigationWaypointId = null
            )
            is SearchDockIntent.NavigationProgressChanged -> state.copy(
                navigationSummary = intent.summary
            )
            is SearchDockIntent.SearchQueryChanged -> {
                val trimmedQuery = intent.query.trim()
                state.copy(
                    searchQuery = intent.query,
                    searchErrorMessage = null,
                    searchResults = if (trimmedQuery.length < MIN_SEARCH_QUERY_LENGTH) {
                        emptyList()
                    } else {
                        state.searchResults
                    },
                    selectedRouteOverride = if (trimmedQuery.isEmpty()) null else state.selectedRouteOverride
                )
            }

            SearchDockIntent.SubmitSearch -> state.copy(
                isExpanded = true,
                isSearching = true,
                searchErrorMessage = null,
                selectedRouteOverride = SearchDockRoute.Search
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
                selectedRouteOverride = SearchDockRoute.Search
            )

            is SearchDockIntent.SearchFailed -> state.copy(
                isSearching = false,
                searchErrorMessage = intent.failure.toMessage(),
                selectedRouteOverride = SearchDockRoute.Search
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

            is SearchDockIntent.OpenMapTapDetail -> state.copy(
                selectedMapTapDetailKey = intent.key
            )

            SearchDockIntent.CloseMapTapDetail -> state.copy(
                selectedMapTapDetailKey = null
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

    /**
     * Auto-expands the dock and routes to [SearchDockRoute.MapTap] iff:
     *   - the user currently has a tapped point,
     *   - the lookup is no longer loading,
     *   - the lookup returned at least one result, AND
     *   - the lookup's [SearchDockIntent.MapTapLookupChanged.cursor] differs
     *     from the cursor we last auto-expanded for (so the dock does not
     *     re-open after the user manually collapses it for this same tap).
     *
     * Always mirrors `hasSelection` into [SearchDockState.hasMapSelection].
     */
    private fun reduceMapTapLookupChanged(
        state: SearchDockState,
        intent: SearchDockIntent.MapTapLookupChanged
    ): SearchDockState {
        // A "fresh" lookup is one whose cursor we have not auto-acted on yet.
        // Once the lookup has finished loading, treat it as a new tap context:
        // dismiss any detail panel from a previous tap so the dock returns to
        // the list view (or stays collapsed for a zero-result tap).
        val isFreshFinishedLookup = intent.hasSelection &&
            !intent.isLoading &&
            intent.cursor != state.lastAutoExpandedMapTapCursor

        val baseDetailKey = if (isFreshFinishedLookup) null else state.selectedMapTapDetailKey

        val base = state.copy(
            hasMapSelection = intent.hasSelection,
            selectedMapTapDetailKey = baseDetailKey
        )

        val shouldAutoExpand = isFreshFinishedLookup && intent.hasResults

        return if (shouldAutoExpand) {
            base.copy(
                isExpanded = true,
                selectedRouteOverride = SearchDockRoute.MapTap,
                lastAutoExpandedMapTapCursor = intent.cursor
            )
        } else {
            base
        }
    }

    private fun Failure.toMessage(): String = when (this) {
        Failure.OutOfCoverage,
        Failure.DataUnavailable,
        is Failure.DataUnavailableReason -> "Data unavailable"

        is Failure.Validation -> message
        Failure.Unexpected -> "Unexpected error"
    }

    private fun SearchDockState.hasNoSpecialState(): Boolean {
        return !isRoutePlanning &&
            !hasMapSelection &&
            searchQuery.isBlank() &&
            searchResults.isEmpty()
    }

    private companion object {
        const val MIN_SEARCH_QUERY_LENGTH = 2
    }
}
