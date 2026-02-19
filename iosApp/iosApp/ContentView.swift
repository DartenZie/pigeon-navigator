import SwiftUI
import CoreLocation
import Shared

struct ContentView: View {
    @State private var coordinate: CLLocationCoordinate2D? = nil
    private let observer = LocationObserver()
    
    var body: some View {
        TabView {
            Tab("Navigate", systemImage: "location.fill") {
                ZStack (alignment: .bottomLeading){
                    NavigateView(location: coordinate, followUser: true)
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
                    
                    HUDCluster(speed: 0, altitude: 0)
                        .padding(.leading)
                        .padding(.bottom, 18)
                }
            }
            Tab("Nearby", systemImage: "mappin.and.ellipse") {
                
            }
            Tab("Settings", systemImage: "gear") {
                
            }
        }
    }
}
