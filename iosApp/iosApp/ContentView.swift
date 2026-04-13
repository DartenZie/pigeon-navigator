import SwiftUI
import CoreLocation
import Shared

struct ContentView: View {
    @State private var coordinate: CLLocationCoordinate2D? = nil
    @State private var mapDirection: CLLocationDirection = 0
    @State private var resetNorthToken: Int = 0
    @State private var recenterOnUserToken: Int = 0
    @State private var isAwayFromUserLocation = false
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
                    topReservedHeight: reservedTop
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
