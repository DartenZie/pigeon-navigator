import SwiftUI
import Shared

/// Picker rows for distance, altitude, and speed units.
///
/// Pure presentation — every change is forwarded as a fully-qualified
/// `(distance, altitude, speed)` triple via `onUnitsChange` because the
/// underlying repository update method requires the complete preference set.
struct UnitsSection: View {
    let distance: DomainDistanceUnit
    let altitude: DomainAltitudeUnit
    let speed: DomainSpeedUnit
    let onUnitsChange: (
        _ distance: DomainDistanceUnit,
        _ altitude: DomainAltitudeUnit,
        _ speed: DomainSpeedUnit
    ) -> Void

    var body: some View {
        Section(header: Text("Units")) {
            Picker("Distance", selection: Binding(
                get: { distance },
                set: { newValue in onUnitsChange(newValue, altitude, speed) }
            )) {
                ForEach(UnitsSection.distanceOptions, id: \.0) { (unit, label) in
                    Text(label).tag(unit)
                }
            }

            Picker("Altitude", selection: Binding(
                get: { altitude },
                set: { newValue in onUnitsChange(distance, newValue, speed) }
            )) {
                ForEach(UnitsSection.altitudeOptions, id: \.0) { (unit, label) in
                    Text(label).tag(unit)
                }
            }

            Picker("Speed", selection: Binding(
                get: { speed },
                set: { newValue in onUnitsChange(distance, altitude, newValue) }
            )) {
                ForEach(UnitsSection.speedOptions, id: \.0) { (unit, label) in
                    Text(label).tag(unit)
                }
            }
        }
    }

    // Static option lists so SwiftUI Pickers can render stable rows. Tuples carry
    // both the Kotlin enum value (for tag/binding) and a short display label.
    private static let distanceOptions: [(DomainDistanceUnit, String)] = [
        (.nauticalmiles, "Nautical miles"),
        (.kilometers, "Kilometers"),
        (.miles, "Miles"),
    ]

    private static let altitudeOptions: [(DomainAltitudeUnit, String)] = [
        (.feet, "Feet"),
        (.meters, "Meters"),
    ]

    private static let speedOptions: [(DomainSpeedUnit, String)] = [
        (.knots, "Knots"),
        (.kilometersperhour, "km/h"),
        (.milesperhour, "mph"),
        (.meterspersecond, "m/s"),
    ]
}
