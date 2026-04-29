import SwiftUI

struct HUDSearchRoutePlannerPage: View {
    @ObservedObject var dock: SearchDockViewModelWrapper

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Route Planner")
                .font(.system(size: 15, weight: .semibold))
            if dock.routeDestinations.isEmpty {
                Text("Add a point to start a route from your current location")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            } else {
                routeRow(label: "A", title: "Current Location")
                ForEach(Array(dock.routeDestinations.enumerated()), id: \.element.id) { index, point in
                    routeRow(
                        label: routeLabel(index + 1),
                        title: point.title,
                        onRemove: { dock.removeRouteDestination(id: point.id) }
                    )
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func routeRow(label: String, title: String, onRemove: (() -> Void)? = nil) -> some View {
        HStack(spacing: 10) {
            Text(label)
                .font(.system(size: 12, weight: .bold))
                .foregroundStyle(.white)
                .frame(width: 24, height: 24)
                .background(Color.blue, in: Circle())
            Text(title)
                .font(.system(size: 13, weight: .medium))
            Spacer(minLength: 0)
            if let onRemove {
                Button(action: onRemove) {
                    Image(systemName: "trash")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(.red)
                        .frame(width: 32, height: 32)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Remove route point")
            }
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 8)
        .background(Color.black.opacity(0.06), in: RoundedRectangle(cornerRadius: 10, style: .continuous))
    }

    private func routeLabel(_ index: Int) -> String {
        guard let scalar = UnicodeScalar(65 + index) else { return "?" }
        return String(Character(scalar))
    }
}
