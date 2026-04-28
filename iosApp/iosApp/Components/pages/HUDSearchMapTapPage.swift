import SwiftUI
import Shared

struct HUDSearchMapTapPage: View {
    @ObservedObject var mapTapLookup: MapTapLookupViewModelWrapper
    @ObservedObject var dock: SearchDockViewModelWrapper
    var onAirportTap: (MapTapAirportItem) -> Void = { _ in }
    var onAirspaceTap: (MapTapAirspaceItem) -> Void = { _ in }
    var onNavaidTap: (MapTapNavaidItem) -> Void = { _ in }
    var onAddToRouteTap: () -> Void = {}

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
                            onTap: { dock.openMapTapDetail("airport:\(airport.id)") },
                            onLocate: { onAirportTap(airport) }
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
                            onTap: { dock.openMapTapDetail("navaid:\(navaid.ident)") },
                            onLocate: { onNavaidTap(navaid) }
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
                            onTap: { dock.openMapTapDetail("airspace:\(airspace.id)") },
                            onLocate: { onAirspaceTap(airspace) }
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
        onTap: @escaping () -> Void,
        onLocate: @escaping () -> Void
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
            Button(action: onLocate) {
                Image(systemName: "mappin.and.ellipse")
                    .font(.system(size: 13, weight: .semibold))
            }
            .buttonStyle(.plain)
            Button(action: onAddToRouteTap) {
                Image(systemName: "plus")
                    .font(.system(size: 13, weight: .semibold))
            }
            .buttonStyle(.plain)
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
        HStack(spacing: 8) {
            Button(action: { dock.closeMapTapDetail() }) {
                Image(systemName: "chevron.backward")
                    .font(.system(size: 14, weight: .semibold))
            }
            .buttonStyle(.plain)

            Text(detail.title)
                .font(.system(size: 16, weight: .semibold))

            Spacer()

            Button(action: { detail.locate(self) }) {
                Image(systemName: "mappin.and.ellipse")
                    .font(.system(size: 14, weight: .semibold))
            }
            .buttonStyle(.plain)

            Button(action: onAddToRouteTap) {
                Image(systemName: "plus")
                    .font(.system(size: 14, weight: .semibold))
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, 4)

        Divider()

        VStack(alignment: .leading, spacing: 8) {
            ForEach(detail.rows, id: \.label) { row in
                HStack(alignment: .firstTextBaseline, spacing: 12) {
                    Text(row.label)
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(.secondary)
                        .frame(minWidth: 86, alignment: .leading)
                    Text(row.value)
                        .font(.system(size: 13))
                }
            }
        }
        .padding(.horizontal, 4)
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
            return [
                DetailRow(label: "Name", value: n.name),
                DetailRow(label: "Detail", value: n.detail),
                DetailRow(label: "Distance", value: n.distanceLabel),
                DetailRow(label: "Position", value: String(format: "%.4f, %.4f", n.latitude, n.longitude))
            ]
        case .airspace(let s):
            return [
                DetailRow(label: "Detail", value: s.detail),
                DetailRow(label: "Bounds NE", value: String(format: "%.4f, %.4f", s.maxLatitude, s.maxLongitude)),
                DetailRow(label: "Bounds SW", value: String(format: "%.4f, %.4f", s.minLatitude, s.minLongitude))
            ]
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
