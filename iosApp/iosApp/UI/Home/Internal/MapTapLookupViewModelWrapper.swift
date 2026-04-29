import SwiftUI
import Shared

/// Bridges `MapTapLookupHandle` from the shared module into a SwiftUI
/// `ObservableObject` so views can observe airport/airspace/navaid lookup
/// state (loading, errors, results) on the main actor.
@MainActor
final class MapTapLookupViewModelWrapper: ObservableObject {
    private let handle: MapTapLookupHandle

    @Published var selectedLatitude: Double? = nil
    @Published var selectedLongitude: Double? = nil
    @Published var isLoading: Bool = false
    @Published var errorMessage: String? = nil
    @Published var airports: [MapTapAirportItem] = []
    @Published var airspaces: [MapTapAirspaceItem] = []
    @Published var navaids: [MapTapNavaidItem] = []

    /// Optional listener invoked on every state update from the shared coordinator.
    /// Used by `HomeScreen` to forward the lookup signal into the dock store so
    /// the shared reducer can auto-expand on a fresh result.
    var onLookupChanged: ((_ cursor: Int64, _ isLoading: Bool, _ hasResults: Bool, _ hasSelection: Bool) -> Void)?

    init() {
        self.handle = MapTapLookupHelper.resolve()
        handle.startState { [weak self] state in
            guard let self else { return }
            self.selectedLatitude = state.selectedLatitude?.doubleValue
            self.selectedLongitude = state.selectedLongitude?.doubleValue
            self.isLoading = state.isLoading
            self.errorMessage = state.errorMessage
            self.airports = state.airports
            self.airspaces = state.airspaces
            self.navaids = state.navaids

            let hasSelection = state.selectedLatitude != nil && state.selectedLongitude != nil
            let hasResults = !state.airports.isEmpty || !state.airspaces.isEmpty || !state.navaids.isEmpty
            self.onLookupChanged?(state.lookupSequence, state.isLoading, hasResults, hasSelection)
        }
    }

    func queryAt(latitude: Double, longitude: Double) {
        handle.queryAt(latitude: latitude, longitude: longitude)
    }

    deinit {
        handle.close()
    }
}
