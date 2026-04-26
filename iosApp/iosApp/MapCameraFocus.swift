import CoreLocation

struct MapCameraFocus {
    let center: CLLocationCoordinate2D
    let bounds: MapCameraBounds?
}

struct MapCameraBounds {
    let southWest: CLLocationCoordinate2D
    let northEast: CLLocationCoordinate2D
}
