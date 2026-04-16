package cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation

data class SearchDockPoiItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val kindLabel: String,
    val distanceMeters: Double
)

data class SearchDockState(
    val isExpanded: Boolean = false,
    val activeRoute: SearchDockRoute = SearchDockRoute.NearbyPoi,
    val availableRoutes: List<SearchDockRoute> = listOf(SearchDockRoute.NearbyPoi),
    val selectedRouteOverride: SearchDockRoute? = null,
    val hasMapSelection: Boolean = false,
    val isRoutePlanning: Boolean = false,
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<String> = emptyList(),
    val searchErrorMessage: String? = null,
    val isNearbyPoiLoading: Boolean = false,
    val nearbyPoiErrorMessage: String? = null,
    val nearbyPoiItems: List<SearchDockPoiItem> = emptyList()
)
