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

    // Local drafts for slider values so we only push to the repository on
    // edit-end events (matches the Android screen's `onValueChangeFinished`).
    @State private var ttcDraft: Double = 60
    @State private var debounceDraft: Double = 300
    @State private var minLengthDraft: Double = 2
    @State private var maxSpeedDraft: Double = 300
    @State private var deltaDraft: Double = 2.5
    @State private var bearingDraft: Double = 4

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
                    HStack {
                        Text("Time-to-collision")
                        Spacer()
                        Text("\(Int(ttcDraft.rounded())) s")
                            .foregroundStyle(.secondary)
                    }
                    Slider(
                        value: $ttcDraft,
                        in: 5...600,
                        step: 1,
                        onEditingChanged: { editing in
                            if !editing {
                                viewModel.updateTimeToCollisionWarningSeconds(
                                    Int32(ttcDraft.rounded())
                                )
                            }
                        }
                    )
                }

                Section(header: Text("Search")) {
                    HStack {
                        Text("Debounce")
                        Spacer()
                        Text("\(Int(debounceDraft.rounded())) ms")
                            .foregroundStyle(.secondary)
                    }
                    Slider(
                        value: $debounceDraft,
                        in: 0...2000,
                        step: 10,
                        onEditingChanged: { editing in
                            if !editing { commitSearch() }
                        }
                    )

                    HStack {
                        Text("Minimum query length")
                        Spacer()
                        Text("\(Int(minLengthDraft.rounded()))")
                            .foregroundStyle(.secondary)
                    }
                    Slider(
                        value: $minLengthDraft,
                        in: 1...10,
                        step: 1,
                        onEditingChanged: { editing in
                            if !editing { commitSearch() }
                        }
                    )
                }

                Section(header: Text("Map")) {
                    HStack {
                        Text("Max dynamic-zoom speed")
                        Spacer()
                        Text("\(Int(maxSpeedDraft.rounded())) km/h")
                            .foregroundStyle(.secondary)
                    }
                    Slider(
                        value: $maxSpeedDraft,
                        in: 50...1500,
                        step: 5,
                        onEditingChanged: { editing in
                            if !editing { commitMap() }
                        }
                    )

                    HStack {
                        Text("Max speed zoom-out delta")
                        Spacer()
                        Text(String(format: "%.2f", deltaDraft))
                            .foregroundStyle(.secondary)
                    }
                    Slider(
                        value: $deltaDraft,
                        in: 0...8,
                        step: 0.05,
                        onEditingChanged: { editing in
                            if !editing { commitMap() }
                        }
                    )

                    HStack {
                        Text("Bearing update threshold")
                        Spacer()
                        Text(String(format: "%.1f°", bearingDraft))
                            .foregroundStyle(.secondary)
                    }
                    Slider(
                        value: $bearingDraft,
                        in: 0...45,
                        step: 0.5,
                        onEditingChanged: { editing in
                            if !editing { commitMap() }
                        }
                    )
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
            .onChange(of: viewModel.searchDebounceMillis) { _ in syncDraftsFromViewModel() }
            .onChange(of: viewModel.minimumQueryLength) { _ in syncDraftsFromViewModel() }
            .onChange(of: viewModel.maxDynamicZoomSpeedKmh) { _ in syncDraftsFromViewModel() }
            .onChange(of: viewModel.maxSpeedZoomOutDelta) { _ in syncDraftsFromViewModel() }
            .onChange(of: viewModel.bearingUpdateThresholdDegrees) { _ in syncDraftsFromViewModel() }
        }
    }

    private func syncDraftsFromViewModel() {
        ttcDraft = Double(viewModel.timeToCollisionWarningSeconds)
        debounceDraft = Double(viewModel.searchDebounceMillis)
        minLengthDraft = Double(viewModel.minimumQueryLength)
        maxSpeedDraft = viewModel.maxDynamicZoomSpeedKmh
        deltaDraft = viewModel.maxSpeedZoomOutDelta
        bearingDraft = viewModel.bearingUpdateThresholdDegrees
    }

    private func commitSearch() {
        viewModel.updateSearchPreferences(
            searchDebounceMillis: Int64(debounceDraft.rounded()),
            minimumQueryLength: Int32(minLengthDraft.rounded())
        )
    }

    private func commitMap() {
        viewModel.updateMapPreferences(
            maxDynamicZoomSpeedKmh: maxSpeedDraft,
            maxSpeedZoomOutDelta: deltaDraft,
            bearingUpdateThresholdDegrees: bearingDraft
        )
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
