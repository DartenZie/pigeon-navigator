import SwiftUI
import Shared

struct SettingsView: View {
    @ObservedObject var settings: AppSettingsViewModelWrapper
    @Environment(\.dismiss) private var dismiss

    private let distanceUnits: [(String, DomainDistanceUnit)] = [
        ("Nautical miles", DomainDistanceUnit.nauticalmiles),
        ("Kilometers", DomainDistanceUnit.kilometers),
        ("Miles", DomainDistanceUnit.miles)
    ]

    private let altitudeUnits: [(String, DomainAltitudeUnit)] = [
        ("Feet", DomainAltitudeUnit.feet),
        ("Meters", DomainAltitudeUnit.meters)
    ]

    private let speedUnits: [(String, DomainSpeedUnit)] = [
        ("Knots", DomainSpeedUnit.knots),
        ("Kilometers per hour", DomainSpeedUnit.kilometersperhour),
        ("Miles per hour", DomainSpeedUnit.milesperhour),
        ("Meters per second", DomainSpeedUnit.meterspersecond)
    ]

    var body: some View {
        NavigationStack {
            Form {
                Section("Units") {
                    Picker(
                        "Distance",
                        selection: Binding(
                            get: { settings.distanceUnit.name },
                            set: { name in
                                if let unit = distanceUnits.first(where: { $0.1.name == name })?.1 {
                                    settings.updateDistanceUnit(unit)
                                }
                            }
                        )
                    ) {
                        ForEach(distanceUnits, id: \.1.name) { label, unit in
                            Text(label).tag(unit.name)
                        }
                    }

                    Picker(
                        "Altitude",
                        selection: Binding(
                            get: { settings.altitudeUnit.name },
                            set: { name in
                                if let unit = altitudeUnits.first(where: { $0.1.name == name })?.1 {
                                    settings.updateAltitudeUnit(unit)
                                }
                            }
                        )
                    ) {
                        ForEach(altitudeUnits, id: \.1.name) { label, unit in
                            Text(label).tag(unit.name)
                        }
                    }

                    Picker(
                        "Speed",
                        selection: Binding(
                            get: { settings.speedUnit.name },
                            set: { name in
                                if let unit = speedUnits.first(where: { $0.1.name == name })?.1 {
                                    settings.updateSpeedUnit(unit)
                                }
                            }
                        )
                    ) {
                        ForEach(speedUnits, id: \.1.name) { label, unit in
                            Text(label).tag(unit.name)
                        }
                    }
                }

                Section("Warnings") {
                    Stepper(
                        "Terrain warning: \(settings.timeToCollisionWarningSeconds) s",
                        value: Binding(
                            get: { Int(settings.timeToCollisionWarningSeconds) },
                            set: { settings.updateTimeToCollisionWarningSeconds(Int32($0)) }
                        ),
                        in: 5...600,
                        step: 5
                    )
                }
            }
            .navigationTitle("Settings")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Done") {
                        dismiss()
                    }
                }
            }
        }
    }
}
