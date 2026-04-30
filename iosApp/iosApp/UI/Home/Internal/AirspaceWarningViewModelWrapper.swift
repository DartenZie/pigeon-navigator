import SwiftUI
import Shared

/// Bridges projected restricted-airspace warnings from shared into SwiftUI state.
@MainActor
final class AirspaceWarningViewModelWrapper: ObservableObject {
    private let handle: AirspaceWarningHandle

    @Published var name: String? = nil
    @Published var minutesBeforeEnter: Int = 0

    init() {
        self.handle = AirspaceWarningHelper.resolve()
    }

    func onLocationUpdated(
        latitude: Double,
        longitude: Double,
        speedMetersPerSecond: Double,
        bearingDegrees: Double
    ) {
        handle.onLocationUpdated(
            latitude: latitude,
            longitude: longitude,
            speedMetersPerSecond: speedMetersPerSecond,
            bearingDegrees: bearingDegrees
        ) { [weak self] state in
            guard let self else { return }
            self.name = state.name
            self.minutesBeforeEnter = Int(state.minutesBeforeEnter)
        }
    }

    deinit {
        handle.close()
    }
}
