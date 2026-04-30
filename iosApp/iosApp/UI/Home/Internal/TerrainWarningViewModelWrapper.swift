import SwiftUI
import CoreLocation
import Shared

/// Bridges `TerrainWarningHandle` from the shared module into a SwiftUI
/// `ObservableObject`. Maps shared severity tags into the iOS-native
/// `TerrainOverlaySeverity` so the map renderer can colour hazard points.
@MainActor
final class TerrainWarningViewModelWrapper: ObservableObject {
    private let handle: TerrainWarningHandle

    @Published var hazardPoints: [TerrainHazardOverlayPoint] = []
    @Published var isCollisionWithinOneMinute: Bool = false

    init() {
        self.handle = TerrainWarningHelper.resolve()
        handle.startState { [weak self] state in
            guard let self else { return }
            self.isCollisionWithinOneMinute = state.isCollisionWithinOneMinute
            self.hazardPoints = state.hazardPoints.compactMap { point in
                guard let severity = TerrainOverlaySeverity(severityTag: point.severity) else {
                    return nil
                }

                return TerrainHazardOverlayPoint(
                    coordinate: CLLocationCoordinate2D(
                        latitude: point.latitude,
                        longitude: point.longitude
                    ),
                    severity: severity
                )
            }
        }
    }

    func onLocationUpdated(
        latitude: Double,
        longitude: Double,
        altitudeMeters: Double,
        speedMetersPerSecond: Double,
        bearingDegrees: Double
    ) {
        handle.onLocationUpdated(
            latitude: latitude,
            longitude: longitude,
            altitudeMeters: altitudeMeters,
            speedMetersPerSecond: speedMetersPerSecond,
            bearingDegrees: bearingDegrees
        )
    }

    deinit {
        handle.close()
    }
}
