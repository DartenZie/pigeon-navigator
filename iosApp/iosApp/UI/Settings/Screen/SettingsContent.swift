import SwiftUI
import Shared

/// Form layout for the settings screen.
///
/// Composes the units and warning sections inside a `Form`. Knows nothing
/// about persistence or navigation — `SettingsScreen` provides those.
struct SettingsContent: View {
    let distanceUnit: DomainDistanceUnit
    let altitudeUnit: DomainAltitudeUnit
    let speedUnit: DomainSpeedUnit
    @Binding var ttcDraft: String
    let onUnitsChange: (
        _ distance: DomainDistanceUnit,
        _ altitude: DomainAltitudeUnit,
        _ speed: DomainSpeedUnit
    ) -> Void
    let onCommitTimeToCollision: (Int32) -> Void

    var body: some View {
        Form {
            UnitsSection(
                distance: distanceUnit,
                altitude: altitudeUnit,
                speed: speedUnit,
                onUnitsChange: onUnitsChange
            )

            WarningSection(
                draft: $ttcDraft,
                onCommitSeconds: onCommitTimeToCollision
            )
        }
    }
}
