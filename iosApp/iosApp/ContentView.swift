import SwiftUI
import CoreLocation
import Shared

struct ContentView: View {
    @State private var coordinate: CLLocationCoordinate2D? = nil
    @State private var mapDirection: CLLocationDirection = 0
    @State private var resetNorthToken: Int = 0
    @State private var recenterOnUserToken: Int = 0
    @State private var isAwayFromUserLocation = false
    @StateObject private var mapTapLookup = MapTapLookupViewModelWrapper()
    private let observer = LocationObserver()
    private let settingsButtonSize: CGFloat = 52
    private let settingsTopPadding: CGFloat = 12
    private let settingsTrailingPadding: CGFloat = 16
    private let settingsBottomGap: CGFloat = 14
    
    var body: some View {
        GeometryReader { proxy in
            let safeInsets = proxy.safeAreaInsets
            let reservedTop = safeInsets.top + settingsTopPadding + settingsButtonSize + settingsBottomGap

            ZStack(alignment: .bottom) {
                NavigateView(
                    location: coordinate,
                    terrainHazardPoints: [],
                    followUser: true,
                    onDirectionChange: { direction in
                        mapDirection = direction
                    },
                    onMapTap: { tapCoordinate in
                        mapTapLookup.queryAt(
                            latitude: tapCoordinate.latitude,
                            longitude: tapCoordinate.longitude
                        )
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
                        }
                    }

                HUDCluster(
                    speed: 0,
                    altitude: 0,
                    mapDirection: mapDirection,
                    isRecenterVisible: isAwayFromUserLocation,
                    onCompassTap: {
                        resetNorthToken += 1
                    },
                    onRecenterTap: {
                        recenterOnUserToken += 1
                    }
                )
                .padding(.horizontal)
                .padding(.bottom, 92)
                .zIndex(1)

                ExpandableSearchPanel(
                    containerSize: proxy.size,
                    topReservedHeight: reservedTop,
                    selectedLatitude: mapTapLookup.selectedLatitude,
                    selectedLongitude: mapTapLookup.selectedLongitude,
                    isLoading: mapTapLookup.isLoading,
                    errorMessage: mapTapLookup.errorMessage,
                    airports: mapTapLookup.airports,
                    airspaces: mapTapLookup.airspaces
                )
                .zIndex(2)

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
                        .padding(.top, safeInsets.top + settingsTopPadding)
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
    @Published var airports: [NearbyAirport] = []
    @Published var airspaces: [Airspace] = []

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
