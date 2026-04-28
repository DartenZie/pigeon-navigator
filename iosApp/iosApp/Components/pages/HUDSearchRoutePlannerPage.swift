import SwiftUI

struct HUDSearchRoutePlannerPage: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Route Planner")
                .font(.system(size: 15, weight: .semibold))
            Text("Build route legs, constraints, and alternates in this view.")
                .font(.footnote)
                .foregroundStyle(.secondary)
            Text("This route is ready for dedicated planner subviews.")
                .font(.footnote)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}
