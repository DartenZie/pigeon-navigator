import Foundation
import Shared

/// Display-side conversions for altitude and speed used by the home HUD.
///
/// Mirrors `composeApp/.../home/model/HudFormatting.kt` so the Android and
/// iOS HUDs render identical numbers.
enum HudFormatting {
    static func displayAltitude(meters: Double, unit: DomainAltitudeUnit) -> Int {
        switch unit {
        case .feet:
            return Int((meters * 3.280839895).rounded())
        case .meters:
            return Int(meters.rounded())
        default:
            return Int(meters.rounded())
        }
    }

    static func displaySpeed(metersPerSecond: Double, unit: DomainSpeedUnit) -> Int {
        switch unit {
        case .knots:
            return Int((metersPerSecond * 1.943844492).rounded())
        case .kilometersperhour:
            return Int((metersPerSecond * 3.6).rounded())
        case .milesperhour:
            return Int((metersPerSecond * 2.236936292).rounded())
        case .meterspersecond:
            return Int(metersPerSecond.rounded())
        default:
            return Int((metersPerSecond * 3.6).rounded())
        }
    }

    static func altitudeUnitLabel(_ unit: DomainAltitudeUnit) -> String {
        switch unit {
        case .feet:
            return "ft"
        case .meters:
            return "m"
        default:
            return "m"
        }
    }

    static func speedUnitLabel(_ unit: DomainSpeedUnit) -> String {
        switch unit {
        case .knots:
            return "kt"
        case .kilometersperhour:
            return "km/h"
        case .milesperhour:
            return "mph"
        case .meterspersecond:
            return "m/s"
        default:
            return "km/h"
        }
    }
}
