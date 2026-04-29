import SwiftUI
import CoreLocation
import UIKit
import Shared

private enum MapInteraction {
    case tap(CLLocationCoordinate2D)
    case pan
}

struct ContentView: View {
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
    @StateObject private var mapTapLookup = MapTapLookupViewModelWrapper()
    @StateObject private var terrainWarning = TerrainWarningViewModelWrapper()
    @StateObject private var dock = SearchDockViewModelWrapper()
    @StateObject private var locationPermission = LocationPermissionController()
    @StateObject private var appSettings = AppSettingsViewModelWrapper()
    private let observer = LocationObserver()
    private let settingsButtonSize: CGFloat = 52
    private let settingsTopPadding: CGFloat = 12
    private let settingsTrailingPadding: CGFloat = 16
    private let hudClusterGapFromSearchBar: CGFloat = 12
    
    var body: some View {
        let speedKmh = locationSpeedMetersPerSecond.isFinite
            ? Int((max(locationSpeedMetersPerSecond, 0) * 3.6).rounded())
            : 0
        let altitudeMeters = locationAltitudeMeters.isFinite
            ? Int(locationAltitudeMeters.rounded())
            : 0

        GeometryReader { proxy in
            let hudSearchBarHeight = max(hudSize.height(in: proxy), 48)

            ZStack(alignment: .bottom) {
                NavigateView(
                    location: coordinate,
                    locationAccuracyMeters: locationAccuracyMeters,
                    locationSpeedMetersPerSecond: locationSpeedMetersPerSecond,
                    locationBearingDegrees: locationBearingDegrees,
                    terrainHazardPoints: terrainWarning.hazardPoints,
                    routeDestinations: dock.routeDestinations,
                    followUser: true,
                    onDirectionChange: { direction in
                        mapDirection = direction
                    },
                    onMapInteraction: {
                        DispatchQueue.main.async {
                            handleMapInteraction(.pan)
                        }
                    },
                    onMapTap: { tapCoordinate in
                        DispatchQueue.main.async {
                            handleMapInteraction(.tap(tapCoordinate))
                        }
                    },
                    onAwayFromUserLocationChange: { isAway in
                        isAwayFromUserLocation = isAway
                    },
                    resetNorthToken: resetNorthToken,
                    recenterOnUserToken: recenterOnUserToken,
                    mapFocus: mapFocus,
                    mapFocusToken: mapFocusToken,
                    bearingUpdateThresholdDegreesOverride: appSettings.bearingUpdateThresholdDegrees,
                    maxDynamicZoomSpeedKmhOverride: appSettings.maxDynamicZoomSpeedKmh,
                    maxSpeedZoomOutDeltaOverride: appSettings.maxSpeedZoomOutDelta
                )
                .ignoresSafeArea()
                .onAppear {
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
                        dock.onUserLocationChanged(
                            latitude: loc.latitude,
                            longitude: loc.longitude
                        )
                    }
                }
                .onDisappear {
                    observer.stop()
                }
                .onChange(of: dock.isExpanded) { (expanded: Bool) in
                    // The shared reducer auto-expands the dock when a fresh
                    // map-tap lookup returns results. Mirror that into the
                    // HUD bar size so the user actually sees the panel grow.
                    // Only act on the collapsed -> expanded transition; the
                    // collapse paths (pan, focusMap) already drive hudSize
                    // explicitly.
                    if expanded && hudSize == .bar {
                        withAnimation(.spring(response: 0.44, dampingFraction: 0.76)) {
                            hudSize = .half
                        }
                    }
                }

                HUDSearchBar(
                    hudSize: $hudSize,
                    dock: dock,
                    mapTapLookup: mapTapLookup,
                    onNearbyPoiTap: { item in
                        focusMap(
                            center: CLLocationCoordinate2D(latitude: item.latitude, longitude: item.longitude)
                        )
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
                    },
                    onAddSearchResultToRouteTap: { item in
                        addRouteDestination(id: item.id, title: item.title, latitude: item.latitude, longitude: item.longitude)
                    },
                    onAddNearbyPoiToRouteTap: { item in
                        addRouteDestination(id: item.id, title: item.title, latitude: item.latitude, longitude: item.longitude)
                    },
                    onAddMapTapToRouteTap: { id, title, latitude, longitude in
                        addRouteDestination(id: id, title: title, latitude: latitude, longitude: longitude)
                    },
                    minimumSearchQueryLength: Int(appSettings.minimumQueryLength),
                    searchDebounceDelay: TimeInterval(appSettings.searchDebounceMillis) / 1000.0
                )
                    .padding(.horizontal, hudSize == .full ? 0 : 16)
                    .zIndex(2)
                    .animation(.spring(response: 0.44, dampingFraction: 0.76), value: hudSize)
                
                if hudSize != .full {
                    HUDCluster(
                        speed: speedKmh,
                        altitude: altitudeMeters,
                        mapDirection: mapDirection,
                        isRecenterVisible: isAwayFromUserLocation,
                        onCompassTap: { resetNorthToken += 1 },
                        onRecenterTap: { recenterOnUserToken += 1 }
                    )
                    .padding(.horizontal)
                    .padding(.bottom, hudSearchBarHeight + hudClusterGapFromSearchBar)
                    .zIndex(1)
                    .transition(.opacity.combined(with: .move(edge: .bottom)))
                    .animation(.spring(response: 0.44, dampingFraction: 0.76), value: hudSize)
                }

                Color.clear
                    .frame(width: 0, height: 0)
                    .onChange(of: dock.isExpanded) { (expanded: Bool) in
                        // Mirror the shared dock state into the HUD chrome size:
                        // when the reducer auto-expands the dock (e.g. after a
                        // map-tap lookup returns results), grow the search bar
                        // to .half. When the dock collapses, shrink back to .bar.
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

                VStack {
                    ZStack(alignment: .trailing) {
                        HStack(alignment: .center) {
                            Spacer(minLength: 0)

                            if let locationStatus, locationStatus != .active {
                                GpsStatusBadge(status: locationStatus) {
                                    if locationStatus == .permissionRequired {
                                        locationPermission.requestOrOpenSettings()
                                    }
                                }
                                .transition(.opacity.combined(with: .move(edge: .top)))
                            }

                            Spacer(minLength: 0)
                        }

                        Button(action: {}) {
                            Image(systemName: "gearshape.fill")
                                .font(.system(size: 20, weight: .semibold))
                                .foregroundStyle(.primary)
                                .frame(width: settingsButtonSize, height: settingsButtonSize)
                                .contentShape(Circle())
                        }
                        .buttonStyle(.plain)
                        .modifier(GlassBubbleStyle(shape: Circle()))
                        .padding(.trailing, settingsTrailingPadding)
                    }
                    .frame(height: settingsButtonSize)
                    .padding(.top, settingsTopPadding)
                    .animation(.spring(response: 0.44, dampingFraction: 0.76), value: locationStatus)

                    Spacer(minLength: 0)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .zIndex(4)
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

    private func addRouteDestination(id: String, title: String, latitude: Double, longitude: Double) {
        dock.addRouteDestination(id: id, title: title, latitude: latitude, longitude: longitude)
        if hudSize == .bar {
            withAnimation(.spring(response: 0.44, dampingFraction: 0.76)) {
                hudSize = .half
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

private enum GpsStatus: Equatable {
    case active
    case permissionRequired
    case signalLost

    init?(kotlinStatusName: String) {
        switch kotlinStatusName {
        case "Active": self = .active
        case "PermissionRequired": self = .permissionRequired
        case "SignalLost": self = .signalLost
        default: return nil
        }
    }
}

private struct GpsStatusBadge: View {
    let status: GpsStatus
    let onTap: () -> Void

    private var title: String {
        switch status {
        case .active: ""
        case .permissionRequired: "No Location Access"
        case .signalLost: "GPS Signal Lost"
        }
    }

    private var systemImage: String {
        switch status {
        case .active: "location.fill"
        case .permissionRequired: "location.slash.fill"
        case .signalLost: "dot.radiowaves.left.and.right"
        }
    }

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 8) {
                Image(systemName: systemImage)
                    .font(.system(size: 15, weight: .semibold))
                Text(title)
                    .font(.system(size: 15, weight: .semibold, design: .rounded))
            }
            .foregroundStyle(status == .permissionRequired ? .red : .orange)
            .padding(.horizontal, 14)
            .padding(.vertical, 10)
            .contentShape(Capsule())
        }
        .buttonStyle(.plain)
        .disabled(status != .permissionRequired)
        .modifier(GlassBubbleStyle(shape: Capsule()))
    }
}

@MainActor
private final class LocationPermissionController: NSObject, ObservableObject, CLLocationManagerDelegate {
    private let manager = CLLocationManager()

    override init() {
        super.init()
        manager.delegate = self
    }

    func requestOrOpenSettings() {
        switch manager.authorizationStatus {
        case .notDetermined:
            manager.requestWhenInUseAuthorization()
        case .denied, .restricted:
            openAppSettings()
        case .authorizedAlways, .authorizedWhenInUse:
            break
        @unknown default:
            break
        }
    }

    private func openAppSettings() {
        guard let settingsURL = URL(string: UIApplication.openSettingsURLString),
              UIApplication.shared.canOpenURL(settingsURL) else {
            return
        }

        UIApplication.shared.open(settingsURL, options: [:])
    }
}

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
    /// Used by `ContentView` to forward the lookup signal into the dock store so
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

@MainActor
final class TerrainWarningViewModelWrapper: ObservableObject {
    private let handle: TerrainWarningHandle

    @Published var hazardPoints: [TerrainHazardOverlayPoint] = []

    init() {
        self.handle = TerrainWarningHelper.resolve()
        handle.startState { [weak self] state in
            guard let self else { return }
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
