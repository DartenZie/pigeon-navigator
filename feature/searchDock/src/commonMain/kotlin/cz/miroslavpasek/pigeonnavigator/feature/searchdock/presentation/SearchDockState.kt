package cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation

import cz.miroslavpasek.pigeonnavigator.domain.search.GeoBounds
import cz.miroslavpasek.pigeonnavigator.domain.search.SearchResult

data class SearchDockPoiItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val kindLabel: String,
    val distanceMeters: Double,
    val latitude: Double,
    val longitude: Double
)

sealed interface SearchDockMapFocus {
    data class Point(
        val latitude: Double,
        val longitude: Double
    ) : SearchDockMapFocus

    data class Bounds(
        val centerLatitude: Double,
        val centerLongitude: Double,
        val bounds: GeoBounds
    ) : SearchDockMapFocus
}

fun SearchDockPoiItem.toMapFocus(): SearchDockMapFocus.Point = SearchDockMapFocus.Point(
    latitude = latitude,
    longitude = longitude
)

fun SearchResult.toMapFocus(): SearchDockMapFocus = when (this) {
    is SearchResult.Airport -> SearchDockMapFocus.Point(latitude = latitude, longitude = longitude)
    is SearchResult.Navaid -> SearchDockMapFocus.Point(latitude = latitude, longitude = longitude)
    is SearchResult.Airspace -> SearchDockMapFocus.Bounds(
        centerLatitude = centerLatitude,
        centerLongitude = centerLongitude,
        bounds = bounds
    )
}

data class SearchDockState(
    val isExpanded: Boolean = false,
    val activeRoute: SearchDockRoute = SearchDockRoute.Nearby,
    val availableRoutes: List<SearchDockRoute> = SearchDockRoute.entries,
    val selectedRouteOverride: SearchDockRoute? = null,
    val hasMapSelection: Boolean = false,
    val isRoutePlanning: Boolean = false,
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<SearchResult> = emptyList(),
    val searchErrorMessage: String? = null,
    val isNearbyPoiLoading: Boolean = false,
    val nearbyPoiErrorMessage: String? = null,
    val nearbyPoiItems: List<SearchDockPoiItem> = emptyList()
)
