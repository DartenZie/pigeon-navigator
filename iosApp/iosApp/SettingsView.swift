import SwiftUI
import Shared

/// Full-screen settings editor backed by the shared `AppSettingsRepository`
/// (via `AppSettingsViewModelWrapper`).
///
/// All range bounds mirror the Kotlin domain constants in
/// `domain/.../settings/AppSettings.kt` and `Units.kt`. Keep them in sync.
struct SettingsView: View {
    @ObservedObject var viewModel: AppSettingsViewModelWrapper
    @Environment(\.dismiss) private var dismiss

    @State private var ttcDraft: String = "60"

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Units")) {
                    Picker("Distance", selection: Binding(
                        get: { viewModel.distanceUnit },
                        set: { newValue in
                            viewModel.updateUnits(
                                distance: newValue,
                                altitude: viewModel.altitudeUnit,
                                speed: viewModel.speedUnit
                            )
                        }
                    )) {
                        ForEach(SettingsView.distanceOptions, id: \.0) { (unit, label) in
                            Text(label).tag(unit)
                        }
                    }

                    Picker("Altitude", selection: Binding(
                        get: { viewModel.altitudeUnit },
                        set: { newValue in
                            viewModel.updateUnits(
                                distance: viewModel.distanceUnit,
                                altitude: newValue,
                                speed: viewModel.speedUnit
                            )
                        }
                    )) {
                        ForEach(SettingsView.altitudeOptions, id: \.0) { (unit, label) in
                            Text(label).tag(unit)
                        }
                    }

                    Picker("Speed", selection: Binding(
                        get: { viewModel.speedUnit },
                        set: { newValue in
                            viewModel.updateUnits(
                                distance: viewModel.distanceUnit,
                                altitude: viewModel.altitudeUnit,
                                speed: newValue
                            )
                        }
                    )) {
                        ForEach(SettingsView.speedOptions, id: \.0) { (unit, label) in
                            Text(label).tag(unit)
                        }
                    }
                }

                Section(header: Text("Terrain warning")) {
                    TextField("Seconds", text: $ttcDraft)
                        .keyboardType(.numberPad)
                        .onChange(of: ttcDraft) { newValue in
                            commitTimeToCollision(newValue)
                        }
                    Text("5-600 seconds")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            }
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Done") { dismiss() }
                }
            }
            .onAppear { syncDraftsFromViewModel() }
            .onChange(of: viewModel.timeToCollisionWarningSeconds) { _ in syncDraftsFromViewModel() }
        }
    }

    private func syncDraftsFromViewModel() {
        ttcDraft = String(viewModel.timeToCollisionWarningSeconds)
    }

    private func commitTimeToCollision(_ value: String) {
        let digitsOnly = value.filter(\.isNumber)
        if digitsOnly != value {
            ttcDraft = digitsOnly
            return
        }
        guard let seconds = Int32(digitsOnly), (5...600).contains(seconds) else { return }
        viewModel.updateTimeToCollisionWarningSeconds(seconds)
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
