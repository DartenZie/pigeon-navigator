package cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation

import cz.miroslavpasek.pigeonnavigator.domain.failure.Failure

sealed interface SearchDockIntent {
    data class ExpandedChanged(val expanded: Boolean) : SearchDockIntent
    data class RouteSelected(val route: SearchDockRoute) : SearchDockIntent
    data class RoutePlanningChanged(val planning: Boolean) : SearchDockIntent
    data class MapSelectionChanged(val hasSelection: Boolean) : SearchDockIntent

    data class UserLocationChanged(
        val latitude: Double,
        val longitude: Double
    ) : SearchDockIntent

    data class SearchQueryChanged(val query: String) : SearchDockIntent
    data object SubmitSearch : SearchDockIntent
    data object SearchCleared : SearchDockIntent
    data class SearchSucceeded(val results: List<String>) : SearchDockIntent
    data class SearchFailed(val failure: Failure) : SearchDockIntent

    data object NearbyPoiLoadRequested : SearchDockIntent
    data class NearbyPoiLoaded(val items: List<SearchDockPoiItem>) : SearchDockIntent
    data class NearbyPoiFailed(val failure: Failure) : SearchDockIntent
}
