import SwiftUI
import CoreLocation
import Shared

/// Top-level composable for the iOS app.
///
/// Owns location, dock, terrain-warning, and map-tap state. Hosts the location
/// observer lifecycle. Delegates layout to `HomeContent`. Keep heavy business
/// logic out — anything not strictly platform-specific belongs in the shared
/// module.
struct HomeScreen: View {
    @State private var coordinate: CLLocationCoordinate2D? = nil
    @State private var locationAccuracyMeters: Double? = nil
    @State private var locationSpeedMetersPerSecond: Double = 0
    @State private var locationAltitudeMeters: Double = 0
    @State private var locationBearingDegrees: Double? = nil
    @State private var mapDirection: CLLocationDirection = 0
    @State private var resetNorthToken: Int = 0
    @State private var recenterOnUserToken: Int = 0
    @State private var mapFocusToken: Int = 0
    @State private var mapFocus: MapCameraFocus? = nil
    @State private var isAwayFromUserLocation = false
    @State private var locationStatus: GpsStatus? = nil
    @State private var hudSize: HUDSize = .bar
    @State private var isSettingsPresented: Bool = false

    @StateObject private var mapTapLookup = MapTapLookupViewModelWrapper()
    @StateObject private var terrainWarning = TerrainWarningViewModelWrapper()
    @StateObject private var airspaceWarning = AirspaceWarningViewModelWrapper()
    @StateObject private var dock = SearchDockViewModelWrapper()
    @StateObject private var locationPermission = LocationPermissionController()
    @ObservedObject var appSettings: AppSettingsViewModelWrapper

    private let observer = LocationObserver()

    var body: some View {
        HomeContent(
            coordinate: coordinate,
            locationAccuracyMeters: locationAccuracyMeters,
            locationSpeedMetersPerSecond: locationSpeedMetersPerSecond,
            locationAltitudeMeters: locationAltitudeMeters,
            locationBearingDegrees: locationBearingDegrees,
            locationStatus: locationStatus,
            mapDirection: mapDirection,
            isAwayFromUserLocation: isAwayFromUserLocation,
            resetNorthToken: resetNorthToken,
            recenterOnUserToken: recenterOnUserToken,
            mapFocus: mapFocus,
            mapFocusToken: mapFocusToken,
            dock: dock,
            mapTapLookup: mapTapLookup,
            terrainWarning: terrainWarning,
            airspaceWarning: airspaceWarning,
            appSettings: appSettings,
            locationPermission: locationPermission,
            hudSize: $hudSize,
            isSettingsPresented: $isSettingsPresented,
            onMapAppear: { startObserving() },
            onMapDisappear: { observer.stop() },
            onMapDirectionChange: { direction in mapDirection = direction },
            onMapInteraction: { handleMapInteraction(.pan) },
            onMapTap: { coordinate in handleMapInteraction(.tap(coordinate)) },
            onAwayFromUserLocationChange: { isAway in isAwayFromUserLocation = isAway },
            onDockExpansionChanged: { expanded in handleDockExpansion(expanded) },
            onResetNorthTap: { resetNorthToken += 1 },
            onRecenterTap: { recenterOnUserToken += 1 },
            onNearbyPoiTap: { item in
                focusMap(center: CLLocationCoordinate2D(latitude: item.latitude, longitude: item.longitude))
            },
            onSearchResultTap: { item in
                focusMap(
                    center: CLLocationCoordinate2D(latitude: item.latitude, longitude: item.longitude),
                    bounds: boundsFromSearchResult(item)
                )
            },
            onMapTapAirportTap: { airport in
                focusMap(
                    center: CLLocationCoordinate2D(latitude: airport.latitude, longitude: airport.longitude)
                )
            },
            onMapTapAirspaceTap: { airspace in
                focusMap(
                    center: CLLocationCoordinate2D(latitude: airspace.latitude, longitude: airspace.longitude),
                    bounds: MapCameraBounds(
                        southWest: CLLocationCoordinate2D(latitude: airspace.minLatitude, longitude: airspace.minLongitude),
                        northEast: CLLocationCoordinate2D(latitude: airspace.maxLatitude, longitude: airspace.maxLongitude)
                    )
                )
            }
        )
    }

    private func startObserving() {
        mapTapLookup.onLookupChanged = { [weak dock = dock] cursor, isLoading, hasResults, hasSelection in
            dock?.onMapTapLookupChanged(
                cursor: cursor,
                isLoading: isLoading,
                hasResults: hasResults,
                hasSelection: hasSelection
            )
        }
        observer.start { loc in
            locationStatus = GpsStatus(kotlinStatusName: loc.status.name)

            guard locationStatus == .active else {
                return
            }

            coordinate = CLLocationCoordinate2D(
                latitude: loc.latitude,
                longitude: loc.longitude
            )
            locationAccuracyMeters = loc.horizontalAccuracyMeters?.doubleValue
            locationSpeedMetersPerSecond = Double(loc.speedMetersPerSecond)
            locationAltitudeMeters = loc.altitudeMeters
            locationBearingDegrees = Double(loc.bearingDegrees)
            terrainWarning.onLocationUpdated(
                latitude: loc.latitude,
                longitude: loc.longitude,
                altitudeMeters: loc.altitudeMeters,
                speedMetersPerSecond: Double(loc.speedMetersPerSecond),
                bearingDegrees: Double(loc.bearingDegrees)
            )
            airspaceWarning.onLocationUpdated(
                latitude: loc.latitude,
                longitude: loc.longitude,
                speedMetersPerSecond: Double(loc.speedMetersPerSecond),
                bearingDegrees: Double(loc.bearingDegrees)
            )
            dock.onUserLocationChanged(
                latitude: loc.latitude,
                longitude: loc.longitude,
                speedMetersPerSecond: Double(loc.speedMetersPerSecond)
            )
        }
    }

    private func handleDockExpansion(_ expanded: Bool) {
        // Mirror the shared dock state into the HUD chrome size:
        // when the reducer auto-expands the dock (e.g. after a map-tap lookup
        // returns results), grow the search bar to .half. When the dock
        // collapses, shrink back to .bar.
        if expanded && hudSize == .bar {
            withAnimation(.spring(response: 0.44, dampingFraction: 0.76)) {
                hudSize = .half
            }
        } else if !expanded && hudSize != .bar {
            withAnimation(.spring(response: 0.44, dampingFraction: 0.76)) {
                hudSize = .bar
            }
        }
    }

    private func handleMapInteraction(_ interaction: MapInteraction) {
        switch interaction {
        case .tap(let coordinate):
            // Run the airspace/airport/navaid lookup. Dock expansion + MapTap
            // routing is decided by the shared reducer once results land.
            mapTapLookup.queryAt(latitude: coordinate.latitude, longitude: coordinate.longitude)
        case .pan:
            dock.onMapSelectionChanged(false)
            dock.onExpandedChanged(false)
        }

        if hudSize != .bar {
            withAnimation(.spring(response: 0.44, dampingFraction: 0.76)) {
                hudSize = .bar
            }
        }
    }

    private func focusMap(center: CLLocationCoordinate2D, bounds: MapCameraBounds? = nil) {
        mapFocus = MapCameraFocus(center: center, bounds: bounds)
        mapFocusToken += 1
        isAwayFromUserLocation = true
        dock.onExpandedChanged(false)
        if hudSize != .bar {
            withAnimation(.spring(response: 0.44, dampingFraction: 0.76)) {
                hudSize = .bar
            }
        }
    }

    private func boundsFromSearchResult(_ item: SearchDockResultViewItem) -> MapCameraBounds? {
        guard let minLatitude = item.minLatitude?.doubleValue,
              let minLongitude = item.minLongitude?.doubleValue,
              let maxLatitude = item.maxLatitude?.doubleValue,
              let maxLongitude = item.maxLongitude?.doubleValue else {
            return nil
        }

        return MapCameraBounds(
            southWest: CLLocationCoordinate2D(latitude: minLatitude, longitude: minLongitude),
            northEast: CLLocationCoordinate2D(latitude: maxLatitude, longitude: maxLongitude)
        )
    }
}
