import CoreLocation

enum TerrainOverlaySeverity {
    case near
    case conflict
}

struct TerrainHazardOverlayPoint {
    let coordinate: CLLocationCoordinate2D
    let severity: TerrainOverlaySeverity
}
