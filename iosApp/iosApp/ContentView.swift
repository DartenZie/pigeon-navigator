import SwiftUI
import CoreLocation
import Shared

struct ContentView: View {
    @State private var coordinate: CLLocationCoordinate2D? = nil
    @State private var locationAccuracyMeters: Double? = nil
    @State private var locationSpeedMetersPerSecond: Double = 0
    @State private var locationAltitudeMeters: Double = 0
    @State private var locationBearingDegrees: Double? = nil
    @State private var mapDirection: CLLocationDirection = 0
    @State private var resetNorthToken: Int = 0
    @State private var recenterOnUserToken: Int = 0
    @State private var isAwayFromUserLocation = false
    @State private var hudSize: HUDSize = .bar
    @StateObject private var mapTapLookup = MapTapLookupViewModelWrapper()
    @StateObject private var terrainWarning = TerrainWarningViewModelWrapper()
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
            let safeInsets = proxy.safeAreaInsets
            let hudSearchBarHeight = max(hudSize.height(in: proxy), 48)

            ZStack(alignment: .bottom) {
                NavigateView(
                    location: coordinate,
                    locationAccuracyMeters: locationAccuracyMeters,
                    locationSpeedMetersPerSecond: locationSpeedMetersPerSecond,
                    locationBearingDegrees: locationBearingDegrees,
                    terrainHazardPoints: terrainWarning.hazardPoints,
                    followUser: true,
                    onDirectionChange: { direction in
                        mapDirection = direction
                    },
                    onMapTap: { tapCoordinate in
                        DispatchQueue.main.async {
                            mapTapLookup.queryAt(
                                latitude: tapCoordinate.latitude,
                                longitude: tapCoordinate.longitude
                            )
                            if hudSize != .bar {
                                withAnimation(.spring(response: 0.44, dampingFraction: 0.76)) {
                                    hudSize = .bar
                                }
                            }
                        }
                    },
                    onAwayFromUserLocationChange: { isAway in
                        isAwayFromUserLocation = isAway
                    },
                    resetNorthToken: resetNorthToken,
                    recenterOnUserToken: recenterOnUserToken
                )
                .ignoresSafeArea()
                .onAppear {
                    observer.start { loc in
                        guard !loc.requiresPermission else {
                            print("Location permission required")
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
                    }
                }
                .onDisappear {
                    observer.stop()
                }
                
                HUDSearchBar(hudSize: $hudSize)
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

                VStack {
                    HStack {
                        Spacer(minLength: 0)

                        Button(action: {}) {
                            Image(systemName: "gearshape.fill")
                                .font(.system(size: 20, weight: .semibold))
                                .foregroundStyle(.primary)
                                .frame(width: settingsButtonSize, height: settingsButtonSize)
                                .contentShape(Circle())
                        }
                        .buttonStyle(.plain)
                        .modifier(GlassBubbleStyle(shape: Circle()))
                        .padding(.top, settingsTopPadding)
                        .padding(.trailing, settingsTrailingPadding)
                    }

                    Spacer(minLength: 0)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .zIndex(3)
            }
        }
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
