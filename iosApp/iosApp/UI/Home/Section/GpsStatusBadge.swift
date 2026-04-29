import SwiftUI

/// Capsule-shaped badge surfaced when GPS is unavailable.
///
/// Becomes interactive only in the `.permissionRequired` state — tapping it
/// sends the user to the system permission prompt or Settings.
struct GpsStatusBadge: View {
    let status: GpsStatus
    let onTap: () -> Void

    private var title: String {
        switch status {
        case .active: ""
        case .permissionRequired: "No Location Access"
        case .signalLost: "GPS Signal Lost"
        }
    }

    private var systemImage: String {
        switch status {
        case .active: "location.fill"
        case .permissionRequired: "location.slash.fill"
        case .signalLost: "dot.radiowaves.left.and.right"
        }
    }

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 8) {
                Image(systemName: systemImage)
                    .font(.system(size: 15, weight: .semibold))
                Text(title)
                    .font(.system(size: 15, weight: .semibold, design: .rounded))
            }
            .foregroundStyle(status == .permissionRequired ? .red : .orange)
            .padding(.horizontal, 14)
            .padding(.vertical, 10)
            .contentShape(Capsule())
        }
        .buttonStyle(.plain)
        .disabled(status != .permissionRequired)
        .modifier(GlassBubbleStyle(shape: Capsule()))
    }
}
