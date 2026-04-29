import SwiftUI
import Shared

struct HUDSearchMapTapPage: View {
    @ObservedObject var mapTapLookup: MapTapLookupViewModelWrapper
    @ObservedObject var dock: SearchDockViewModelWrapper
    var onAirportTap: (MapTapAirportItem) -> Void = { _ in }
    var onAirspaceTap: (MapTapAirspaceItem) -> Void = { _ in }
    var onNavaidTap: (MapTapNavaidItem) -> Void = { _ in }
    var onAddToRouteTap: (String, String, Double, Double) -> Void = { _, _, _, _ in }

    private var detailRecord: MapTapDetailRecord? {
        guard let key = dock.selectedMapTapDetailKey else { return nil }
        return findRecord(key: key, lookup: mapTapLookup)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            if let detail = detailRecord {
                detailView(detail: detail)
            } else {
                listView
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    // MARK: - List view

    @ViewBuilder
    private var listView: some View {
        Text("Map Tap Details")
            .font(.system(size: 15, weight: .semibold))

        if mapTapLookup.isLoading {
            ProgressView("Fetching nearby data...")
                .frame(maxWidth: .infinity, alignment: .leading)
        }

        if let errorMessage = mapTapLookup.errorMessage {
            Text(errorMessage)
                .font(.footnote)
                .foregroundStyle(.red)
                .frame(maxWidth: .infinity, alignment: .leading)
        }

        if !mapTapLookup.airports.isEmpty {
            Group {
                Text("Airports")
                    .font(.system(size: 13, weight: .semibold))
                LazyVStack(spacing: 8) {
                    ForEach(mapTapLookup.airports, id: \.id) { airport in
                        rowContainer(
                            iconName: "airplane",
                            title: airport.id,
                            subtitle: airport.name,
                            trailingLabel: airport.distanceLabel,
                            onTap: { dock.openMapTapDetail("airport:\(airport.id)") }
                        )
                    }
                }
            }
        }

        if !mapTapLookup.navaids.isEmpty {
            Group {
                Text("Navaids")
                    .font(.system(size: 13, weight: .semibold))
                LazyVStack(spacing: 8) {
                    ForEach(mapTapLookup.navaids, id: \.id) { navaid in
                        rowContainer(
                            iconName: "antenna.radiowaves.left.and.right",
                            title: navaid.ident,
                            subtitle: navaid.detail,
                            trailingLabel: navaid.distanceLabel,
                            onTap: { dock.openMapTapDetail("navaid:\(navaid.ident)") }
                        )
                    }
                }
            }
        }

        if !mapTapLookup.airspaces.isEmpty {
            Group {
                Text("Airspaces")
                    .font(.system(size: 13, weight: .semibold))
                LazyVStack(spacing: 8) {
                    ForEach(mapTapLookup.airspaces, id: \.id) { airspace in
                        rowContainer(
                            iconName: nil,
                            title: airspace.name,
                            subtitle: airspace.detail,
                            trailingLabel: nil,
                            onTap: { dock.openMapTapDetail("airspace:\(airspace.id)") }
                        )
                    }
                }
            }
        }
    }

    @ViewBuilder
    private func rowContainer(
        iconName: String?,
        title: String,
        subtitle: String,
        trailingLabel: String?,
        onTap: @escaping () -> Void
    ) -> some View {
        HStack(spacing: 8) {
            if let iconName {
                Image(systemName: iconName)
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(.secondary)
            }
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.system(size: 13, weight: .semibold))
                Text(subtitle)
                    .font(.system(size: 12))
                    .foregroundStyle(.secondary)
            }
            Spacer()
            if let trailingLabel {
                Text(trailingLabel)
                    .font(.system(size: 12))
                    .foregroundStyle(.secondary)
            }
        }
        .contentShape(Rectangle())
        .onTapGesture { onTap() }
        .padding(.horizontal, 10)
        .padding(.vertical, 8)
        .background(Color.black.opacity(0.06), in: RoundedRectangle(cornerRadius: 10, style: .continuous))
    }

    // MARK: - Detail view

    @ViewBuilder
    private func detailView(detail: MapTapDetailRecord) -> some View {
        detailHeader(title: detail.title, onBack: { dock.closeMapTapDetail() })

        Divider()

        VStack(alignment: .leading, spacing: 8) {
            ForEach(detail.rows, id: \.label) { row in
                detailRow(label: row.label, value: row.value)
            }
        }
        .padding(.horizontal, 4)

        VStack(spacing: 8) {
            detailActionButton(title: "Locate on Map", systemImage: "mappin.and.ellipse") { detail.locate(self) }
            detailActionButton(title: "Add to Route", systemImage: "plus") {
                onAddToRouteTap(detail.routeId, detail.title, detail.latitude, detail.longitude)
            }
        }
        .padding(.top, 4)
    }

    fileprivate func locateAirport(_ a: MapTapAirportItem) { onAirportTap(a) }
    fileprivate func locateNavaid(_ n: MapTapNavaidItem) { onNavaidTap(n) }
    fileprivate func locateAirspace(_ s: MapTapAirspaceItem) { onAirspaceTap(s) }
}

private struct DetailRow {
    let label: String
    let value: String
}

private enum MapTapDetailRecord {
    case airport(MapTapAirportItem)
    case navaid(MapTapNavaidItem)
    case airspace(MapTapAirspaceItem)

    var title: String {
        switch self {
        case .airport(let a): return a.id
        case .navaid(let n): return n.ident
        case .airspace(let s): return s.name
        }
    }

    var rows: [DetailRow] {
        switch self {
        case .airport(let a):
            return [
                DetailRow(label: "Name", value: a.name),
                DetailRow(label: "Distance", value: a.distanceLabel),
                DetailRow(label: "Position", value: String(format: "%.4f, %.4f", a.latitude, a.longitude))
            ]
        case .navaid(let n):
            var rows = [
                DetailRow(label: "Name", value: n.name),
                DetailRow(label: "Detail", value: n.detail)
            ]
            if let frequency = n.frequency, !frequency.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                rows.append(DetailRow(label: "Frequency", value: frequency))
            }
            rows.append(contentsOf: [
                DetailRow(label: "Distance", value: n.distanceLabel),
                DetailRow(label: "Position", value: String(format: "%.4f, %.4f", n.latitude, n.longitude))
            ])
            return rows
        case .airspace(let s):
            return [
                DetailRow(label: "Detail", value: s.detail),
                DetailRow(label: "Bounds NE", value: String(format: "%.4f, %.4f", s.maxLatitude, s.maxLongitude)),
                DetailRow(label: "Bounds SW", value: String(format: "%.4f, %.4f", s.minLatitude, s.minLongitude))
            ]
        }
    }

    var routeId: String {
        switch self {
        case .airport(let a): return "airport:\(a.id)"
        case .navaid(let n): return "navaid:\(n.ident)"
        case .airspace(let s): return "airspace:\(s.id)"
        }
    }

    var latitude: Double {
        switch self {
        case .airport(let a): return a.latitude
        case .navaid(let n): return n.latitude
        case .airspace(let s): return s.latitude
        }
    }

    var longitude: Double {
        switch self {
        case .airport(let a): return a.longitude
        case .navaid(let n): return n.longitude
        case .airspace(let s): return s.longitude
        }
    }

    func locate(_ page: HUDSearchMapTapPage) {
        switch self {
        case .airport(let a): page.locateAirport(a)
        case .navaid(let n): page.locateNavaid(n)
        case .airspace(let s): page.locateAirspace(s)
        }
    }
}

@MainActor
private func findRecord(key: String, lookup: MapTapLookupViewModelWrapper) -> MapTapDetailRecord? {
    if key.hasPrefix("airport:") {
        let id = String(key.dropFirst("airport:".count))
        if let match = lookup.airports.first(where: { $0.id == id }) {
            return .airport(match)
        }
    } else if key.hasPrefix("navaid:") {
        let id = String(key.dropFirst("navaid:".count))
        if let match = lookup.navaids.first(where: { $0.ident == id }) {
            return .navaid(match)
        }
    } else if key.hasPrefix("airspace:") {
        let id = String(key.dropFirst("airspace:".count))
        if let match = lookup.airspaces.first(where: { $0.id == id }) {
            return .airspace(match)
        }
    }
    return nil
}
