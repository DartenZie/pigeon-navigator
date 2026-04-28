package cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation

import cz.miroslavpasek.pigeonnavigator.domain.search.GeoBounds
import cz.miroslavpasek.pigeonnavigator.domain.search.SearchResult

data class SearchDockPoiItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val kindLabel: String,
    val frequency: String? = null,
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
    val nearbyPoiItems: List<SearchDockPoiItem> = emptyList(),
    /**
     * Cursor of the last [MapTapLookup] for which the dock was auto-expanded.
     * Internal bookkeeping for the reducer's "expand only on a fresh lookup
     * with results" rule; UI does not need to read this.
     */
    val lastAutoExpandedMapTapCursor: Long? = null,
    /**
     * Stable identifier of the map-tap result currently shown in the detail
     * panel (e.g. `"airport:LKAA"`, `"navaid:PRG"`, `"airspace:LKAA-CTR"`),
     * or `null` when the dock is showing the regular list of categories.
     *
     * The UI looks the actual record up by this key in the latest
     * `MapTapLookupState`. The reducer auto-clears the key when a fresh
     * lookup arrives or the dock is collapsed.
     */
    val selectedMapTapDetailKey: String? = null
)
