import SwiftUI
import Shared

/// Full-screen settings editor backed by the shared `AppSettingsRepository`
/// (via `AppSettingsViewModelWrapper`).
///
/// All range bounds mirror the Kotlin domain constants in
/// `domain/.../settings/AppSettings.kt` and `Units.kt`. Keep them in sync.
///
/// This is the platform `Screen` entry point: it owns navigation, the
/// dismiss environment, and the `ttcDraft` input state, then delegates the
/// actual layout to `SettingsContent`.
struct SettingsScreen: View {
    @ObservedObject var viewModel: AppSettingsViewModelWrapper
    @Environment(\.dismiss) private var dismiss

    @State private var ttcDraft: String = "60"

    var body: some View {
        NavigationView {
            SettingsContent(
                distanceUnit: viewModel.distanceUnit,
                altitudeUnit: viewModel.altitudeUnit,
                speedUnit: viewModel.speedUnit,
                ttcDraft: $ttcDraft,
                onUnitsChange: { distance, altitude, speed in
                    viewModel.updateUnits(distance: distance, altitude: altitude, speed: speed)
                },
                onCommitTimeToCollision: { seconds in
                    viewModel.updateTimeToCollisionWarningSeconds(seconds)
                }
            )
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
}
