import CoreLocation

enum TerrainOverlaySeverity {
    case near
    case conflict
}

struct TerrainHazardOverlayPoint {
    let coordinate: CLLocationCoordinate2D
    let severity: TerrainOverlaySeverity
}

extension TerrainOverlaySeverity {
    init?(severityTag: String) {
        switch severityTag {
        case "near":
            self = .near
        case "conflict":
            self = .conflict
        default:
            return nil
        }
    }
}
