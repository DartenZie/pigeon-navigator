import SwiftUI
import Combine
import Shared

/// Swift-native mirror of the shared `SearchDockRoute` enum. Uses string tags at the
/// Kotlin/Swift boundary so the iOS code avoids the auto-prefixed `SDF...` class name
/// and lowercased enum cases that Kotlin/Native generates for feature modules.
enum HUDSearchRoute: String, CaseIterable, Hashable {
    case nearby
    case search
    case routePlanner
    case mapTap

    init(tag: String) {
        self = HUDSearchRoute(rawValue: tag) ?? .nearby
    }

    var tag: String { rawValue }

    var title: String {
        switch self {
        case .nearby: return "Nearby"
        case .search: return "Search"
        case .routePlanner: return "Route"
        case .mapTap: return "Map Tap"
        }
    }

    var systemImageName: String {
        switch self {
        case .nearby: return "location"
        case .search: return "magnifyingglass"
        case .routePlanner: return "point.topleft.down.curvedto.point.bottomright.up"
        case .mapTap: return "mappin.and.ellipse"
        }
    }
}

/// Bridges the shared Kotlin `SearchDockHandle` into an observable SwiftUI model.
/// Owns the HUD search bar routing state that used to live in ad-hoc Swift state.
@MainActor
final class SearchDockViewModelWrapper: ObservableObject {
    private let handle: SearchDockHandle

    @Published var isExpanded: Bool = false
    @Published var activeRoute: HUDSearchRoute = .nearby
    @Published var availableRoutes: [HUDSearchRoute] = HUDSearchRoute.allCases
    @Published var isRoutePlanning: Bool = false
    @Published var hasMapSelection: Bool = false
    @Published var query: String = ""
    @Published var isSearching: Bool = false
    @Published var results: [SearchDockResultViewItem] = []
    @Published var errorMessage: String? = nil
    @Published var isNearbyPoiLoading: Bool = false
    @Published var nearbyPoiErrorMessage: String? = nil
    @Published var nearbyPoiItems: [SearchDockPoiViewItem] = []
    @Published var selectedMapTapDetailKey: String? = nil

    init() {
        self.handle = SearchDockHelper.resolve()
        handle.startState { [weak self] state in
            guard let self else { return }
            self.isExpanded = state.isExpanded
            self.activeRoute = HUDSearchRoute(tag: state.activeRouteTag)
            self.availableRoutes = state.availableRouteTags.map { HUDSearchRoute(tag: $0) }
            self.isRoutePlanning = state.isRoutePlanning
            self.hasMapSelection = state.hasMapSelection
            if self.query != state.searchQuery {
                self.query = state.searchQuery
            }
            self.isSearching = state.isSearching
            self.results = state.searchResults
            self.errorMessage = state.searchErrorMessage
            self.isNearbyPoiLoading = state.isNearbyPoiLoading
            self.nearbyPoiErrorMessage = state.nearbyPoiErrorMessage
            self.nearbyPoiItems = state.nearbyPoiItems
            self.selectedMapTapDetailKey = state.selectedMapTapDetailKey
        }
    }

    func onQueryChange(_ newQuery: String) {
        handle.onQueryChanged(query: newQuery)
    }

    func submitSearch() {
        handle.submitSearch()
    }

    func clearSearch() {
        handle.clearSearch()
    }

    func onExpandedChanged(_ expanded: Bool) {
        handle.onExpandedChanged(expanded: expanded)
    }

    func selectRoute(_ route: HUDSearchRoute) {
        handle.selectRoute(routeTag: route.tag)
    }

    func onRoutePlanningChanged(_ planning: Bool) {
        handle.onRoutePlanningChanged(planning: planning)
    }

    func onMapSelectionChanged(_ hasSelection: Bool) {
        handle.onMapSelectionChanged(hasSelection: hasSelection)
    }

    /// Forwards the latest map-tap lookup state to the shared dock reducer.
    /// The reducer auto-expands the dock and switches to the MapTap route
    /// the first time a fresh `cursor` arrives with `hasResults == true`.
    func onMapTapLookupChanged(
        cursor: Int64,
        isLoading: Bool,
        hasResults: Bool,
        hasSelection: Bool
    ) {
        handle.onMapTapLookupChanged(
            cursor: cursor,
            isLoading: isLoading,
            hasResults: hasResults,
            hasSelection: hasSelection
        )
    }

    func onUserLocationChanged(latitude: Double, longitude: Double) {
        handle.onUserLocationChanged(latitude: latitude, longitude: longitude)
    }

    /// Opens the map-tap detail panel for the given key (e.g. "airport:LKAA").
    func openMapTapDetail(_ key: String) {
        handle.openMapTapDetail(key: key)
    }

    /// Dismisses the map-tap detail panel and returns to the list view.
    func closeMapTapDetail() {
        handle.closeMapTapDetail()
    }

    deinit {
        handle.close()
    }
}
