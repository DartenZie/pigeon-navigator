import SwiftUI
import CoreLocation
import Shared

/// Top-level visual layout for the home screen.
///
/// Stacks the moving map, search bar, HUD cluster, status row, and the
/// settings sheet trigger. Pure composition — `HomeScreen` owns all of the
/// state and feeds it down through the parameters/bindings on this view.
struct HomeContent: View {
    // Location-derived state
    let coordinate: CLLocationCoordinate2D?
    let locationAccuracyMeters: Double?
    let locationSpeedMetersPerSecond: Double
    let locationAltitudeMeters: Double
    let locationBearingDegrees: Double?
    let locationStatus: GpsStatus?
    let mapDirection: CLLocationDirection
    let isAwayFromUserLocation: Bool

    // Map control tokens / focus
    let resetNorthToken: Int
    let recenterOnUserToken: Int
    let mapFocus: MapCameraFocus?
    let mapFocusToken: Int

    // Dock + map-tap state
    @ObservedObject var dock: SearchDockViewModelWrapper
    @ObservedObject var mapTapLookup: MapTapLookupViewModelWrapper
    @ObservedObject var terrainWarning: TerrainWarningViewModelWrapper
    @ObservedObject var appSettings: AppSettingsViewModelWrapper
    @ObservedObject var locationPermission: LocationPermissionController

    @Binding var hudSize: HUDSize
    @Binding var isSettingsPresented: Bool

    // Lifecycle / event callbacks
    let onMapAppear: () -> Void
    let onMapDisappear: () -> Void
    let onMapDirectionChange: (CLLocationDirection) -> Void
    let onMapInteraction: () -> Void
    let onMapTap: (CLLocationCoordinate2D) -> Void
    let onAwayFromUserLocationChange: (Bool) -> Void
    let onDockExpansionChanged: (Bool) -> Void
    let onResetNorthTap: () -> Void
    let onRecenterTap: () -> Void
    let onNearbyPoiTap: (SearchDockPoiViewItem) -> Void
    let onSearchResultTap: (SearchDockResultViewItem) -> Void
    let onMapTapAirportTap: (MapTapAirportItem) -> Void
    let onMapTapAirspaceTap: (MapTapAirspaceItem) -> Void

    private let settingsButtonSize: CGFloat = 52
    private let settingsTopPadding: CGFloat = 12
    private let settingsTrailingPadding: CGFloat = 16
    private let hudClusterGapFromSearchBar: CGFloat = 12

    var body: some View {
        let speed = locationSpeedMetersPerSecond.isFinite
            ? HudFormatting.displaySpeed(metersPerSecond: max(locationSpeedMetersPerSecond, 0), unit: appSettings.speedUnit)
            : 0
        let altitude = locationAltitudeMeters.isFinite
            ? HudFormatting.displayAltitude(meters: locationAltitudeMeters, unit: appSettings.altitudeUnit)
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
                    onDirectionChange: onMapDirectionChange,
                    onMapInteraction: {
                        DispatchQueue.main.async { onMapInteraction() }
                    },
                    onMapTap: { tapCoordinate in
                        DispatchQueue.main.async { onMapTap(tapCoordinate) }
                    },
                    onAwayFromUserLocationChange: onAwayFromUserLocationChange,
                    resetNorthToken: resetNorthToken,
                    recenterOnUserToken: recenterOnUserToken,
                    mapFocus: mapFocus,
                    mapFocusToken: mapFocusToken,
                    bearingUpdateThresholdDegreesOverride: appSettings.bearingUpdateThresholdDegrees,
                    maxDynamicZoomSpeedKmhOverride: appSettings.maxDynamicZoomSpeedKmh,
                    maxSpeedZoomOutDeltaOverride: appSettings.maxSpeedZoomOutDelta
                )
                .ignoresSafeArea()
                .onAppear { onMapAppear() }
                .onDisappear { onMapDisappear() }

                HUDSearchBar(
                    hudSize: $hudSize,
                    dock: dock,
                    mapTapLookup: mapTapLookup,
                    onNearbyPoiTap: onNearbyPoiTap,
                    onSearchResultTap: onSearchResultTap,
                    onMapTapAirportTap: onMapTapAirportTap,
                    onMapTapAirspaceTap: onMapTapAirspaceTap,
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
                        speed: speed,
                        speedUnit: HudFormatting.speedUnitLabel(appSettings.speedUnit),
                        altitude: altitude,
                        altitudeUnit: HudFormatting.altitudeUnitLabel(appSettings.altitudeUnit),
                        mapDirection: mapDirection,
                        isRecenterVisible: isAwayFromUserLocation,
                        onCompassTap: onResetNorthTap,
                        onRecenterTap: onRecenterTap
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
                        onDockExpansionChanged(expanded)
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

                        Button(action: { isSettingsPresented = true }) {
                            Image(systemName: "gearshape.fill")
                                .font(.system(size: 20, weight: .semibold))
                                .foregroundStyle(.primary)
                                .frame(width: settingsButtonSize, height: settingsButtonSize)
                                .contentShape(Circle())
                        }
                        .buttonStyle(.plain)
                        .modifier(GlassBubbleStyle(shape: Circle()))
                        .padding(.trailing, settingsTrailingPadding)
                        .sheet(isPresented: $isSettingsPresented) {
                            SettingsScreen(viewModel: appSettings)
                        }
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

    private func addRouteDestination(id: String, title: String, latitude: Double, longitude: Double) {
        dock.addRouteDestination(id: id, title: title, latitude: latitude, longitude: longitude)
        if hudSize == .bar {
            withAnimation(.spring(response: 0.44, dampingFraction: 0.76)) {
                hudSize = .half
            }
        }
    }
}
