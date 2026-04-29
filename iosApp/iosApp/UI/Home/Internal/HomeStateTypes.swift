import SwiftUI
import CoreLocation
import UIKit

/// Discriminated input for the home screen's map interactions.
enum MapInteraction {
    case tap(CLLocationCoordinate2D)
    case pan
}

/// Lightweight mirror of the Kotlin `LocationStatus` enum so SwiftUI can
/// switch on a Swift native value.
enum GpsStatus: Equatable {
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

/// Owns the `CLLocationManager` used to escalate the user from "permission
/// required" to either the iOS prompt or the system Settings deep link.
@MainActor
final class LocationPermissionController: NSObject, ObservableObject, CLLocationManagerDelegate {
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
