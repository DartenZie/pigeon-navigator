package cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation

import cz.miroslavpasek.pigeonnavigator.domain.failure.Failure
import cz.miroslavpasek.pigeonnavigator.domain.search.SearchResult

sealed interface SearchDockIntent {
    data class ExpandedChanged(val expanded: Boolean) : SearchDockIntent
    data class RouteSelected(val route: SearchDockRoute) : SearchDockIntent
    data class RoutePlanningChanged(val planning: Boolean) : SearchDockIntent
    data class MapSelectionChanged(val hasSelection: Boolean) : SearchDockIntent

    /**
     * Forwarded from the platform layer whenever the map-tap lookup state changes.
     * Drives the auto-expand-and-route-to-MapTap rule when a fresh lookup
     * (identified by [cursor]) completes with at least one result.
     *
     * @param cursor identifier of the underlying lookup; the reducer uses it to
     *   avoid re-expanding the dock for the same lookup after the user manually
     *   collapses it.
     * @param isLoading true while the underlying lookup is in flight.
     * @param hasResults true if at least one airport, airspace, or navaid was
     *   returned for the tapped point.
     * @param hasSelection true if the user currently has a tapped point.
     */
    data class MapTapLookupChanged(
        val cursor: Long,
        val isLoading: Boolean,
        val hasResults: Boolean,
        val hasSelection: Boolean
    ) : SearchDockIntent

    data class UserLocationChanged(
        val latitude: Double,
        val longitude: Double
    ) : SearchDockIntent

    data class RouteDestinationAdded(val point: SearchDockRoutePoint) : SearchDockIntent
    data class RouteDestinationRemoved(val id: String) : SearchDockIntent

    data class SearchQueryChanged(val query: String) : SearchDockIntent
    data object SubmitSearch : SearchDockIntent
    data object SearchCleared : SearchDockIntent
    data class SearchSucceeded(val results: List<SearchResult>) : SearchDockIntent
    data class SearchFailed(val failure: Failure) : SearchDockIntent

    data object NearbyPoiLoadRequested : SearchDockIntent
    data class NearbyPoiLoaded(val items: List<SearchDockPoiItem>) : SearchDockIntent
    data class NearbyPoiFailed(val failure: Failure) : SearchDockIntent

    /**
     * Opens the detail panel for the map-tap result identified by [key]
     * (e.g. `"airport:LKAA"`). The matching record is looked up by the UI in
     * the latest `MapTapLookupState`. If [key] does not exist in the current
     * lookup the UI falls back to the list view.
     */
    data class OpenMapTapDetail(val key: String) : SearchDockIntent

    /** Closes the map-tap detail panel and returns to the list view. */
    data object CloseMapTapDetail : SearchDockIntent
}
