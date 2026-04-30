import SwiftUI
import Shared

struct HUDNavigationDetailPage: View {
    @ObservedObject var dock: SearchDockViewModelWrapper

    var body: some View {
        VStack(spacing: 12) {
            if let waypoint = dock.selectedNavigationWaypoint {
                navigationWaypointDetail(waypoint)
            } else {
                VStack(spacing: 12) {
                    if let waypoint = dock.nextWaypoint {
                        NavigationActionButton(
                            title: navigationLabel(for: waypoint),
                            iconName: waypointKind(for: waypoint).iconName,
                            iconColor: waypointKind(for: waypoint).color,
                            action: {
                                dock.openNavigationWaypointDetail(id: waypoint.id)
                            }
                        )
                    }

                    NavigationActionButton(
                        title: "Add waypoint",
                        iconName: "plus.circle.fill",
                        iconColor: .blue,
                        action: {
                            dock.addWaypoint()
                        }
                    )

                    Button(role: .destructive) {
                        dock.endFlight()
                    } label: {
                        Text("End flight")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(.red)
                    .controlSize(.large)
                }
                .padding(.bottom, 16)
            }
        }
        .frame(maxWidth: .infinity)
    }

    @ViewBuilder
    private func navigationWaypointDetail(_ waypoint: SearchDockRoutePointViewItem) -> some View {
        detailHeader(title: navigationLabel(for: waypoint), onBack: { dock.closeNavigationDetail() })

        Divider()

        VStack(alignment: .leading, spacing: 8) {
            detailRow(label: "Kind", value: waypointKind(for: waypoint).label)
            detailRow(label: "Name", value: waypoint.title)
            detailRow(label: "Position", value: String(format: "%.4f, %.4f", waypoint.latitude, waypoint.longitude))
        }
        .padding(.horizontal, 4)
    }

    private func navigationLabel(for waypoint: SearchDockRoutePointViewItem) -> String {
        let code = waypoint.id.split(separator: ":").last.map(String.init) ?? waypoint.title
        return "\(waypoint.title) (\(code))"
    }

    private func waypointKind(for waypoint: SearchDockRoutePointViewItem) -> WaypointKind {
        if waypoint.id.hasPrefix("airport:") { return .airport }
        if waypoint.id.hasPrefix("airspace:") { return .airspace }
        if waypoint.id.hasPrefix("navaid:") { return .navaid }
        return .lonLat
    }
}

private enum WaypointKind {
    case airport, airspace, navaid, lonLat

    var label: String {
        switch self {
        case .airport: return "Airport"
        case .airspace: return "Airspace"
        case .navaid: return "Navaid"
        case .lonLat: return "Lon/lat"
        }
    }

    var color: Color {
        switch self {
        case .airport: return .green
        case .airspace: return .red
        case .navaid: return .blue
        case .lonLat: return .yellow
        }
    }

    var iconName: String {
        switch self {
        case .airport: return "airplane.departure"
        case .airspace: return "shield.lefthalf.filled"
        case .navaid: return "antenna.radiowaves.left.and.right"
        case .lonLat: return "mappin.and.ellipse"
        }
    }
}

private struct NavigationActionButton: View {
    let title: String
    let iconName: String
    let iconColor: Color
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                Image(systemName: iconName)
                    .font(.system(size: 17, weight: .bold))
                    .foregroundStyle(iconColor)
                    .frame(width: 24)

                Text(title)
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(.black)
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)

                Spacer(minLength: 0)
            }
            .padding(.horizontal, 16)
            .frame(maxWidth: .infinity, minHeight: 58)
            .background(Color.white, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .stroke(Color.black.opacity(0.07), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }
}
