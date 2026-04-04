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
    
    var body: some View {
        ZStack(alignment: .bottom) {
            NavigateView(
                location: coordinate,
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
            .padding(.bottom, 18)
        }
    }
}
