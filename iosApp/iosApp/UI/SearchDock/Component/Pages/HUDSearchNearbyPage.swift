import SwiftUI
import Shared

struct HUDSearchNearbyPage: View {
    @ObservedObject var dock: SearchDockViewModelWrapper
    var onPoiTap: (SearchDockPoiViewItem) -> Void = { _ in }
    var onAddToRouteTap: (SearchDockPoiViewItem) -> Void = { _ in }

    @State private var selectedPoiID: String? = nil

    private var selectedPoi: SearchDockPoiViewItem? {
        guard let selectedPoiID else { return nil }
        return dock.nearbyPoiItems.first { $0.id == selectedPoiID }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            if let selectedPoi {
                detailView(item: selectedPoi)
            } else {
                listView
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .onReceive(dock.$nearbyPoiItems) { items in
            if let selectedPoiID, !items.contains(where: { $0.id == selectedPoiID }) {
                self.selectedPoiID = nil
            }
        }
    }

    @ViewBuilder
    private var listView: some View {
        Text("Nearby")
            .font(.system(size: 15, weight: .semibold))

        if dock.isNearbyPoiLoading {
            ProgressView("Fetching nearby POIs...")
                .frame(maxWidth: .infinity, alignment: .leading)
        }

        if let errorMessage = dock.nearbyPoiErrorMessage {
            Text(errorMessage)
                .font(.footnote)
                .foregroundStyle(.red)
                .frame(maxWidth: .infinity, alignment: .leading)
        }

        if dock.nearbyPoiItems.isEmpty && !dock.isNearbyPoiLoading {
            Text("No nearby POIs available")
                .font(.footnote)
                .foregroundStyle(.secondary)
                .frame(maxWidth: .infinity, alignment: .leading)
        } else {
            LazyVStack(spacing: 8) {
                ForEach(dock.nearbyPoiItems, id: \.id) { item in
                    HStack(spacing: 8) {
                        Image(systemName: "mappin")
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundStyle(.secondary)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(item.title)
                                .font(.system(size: 13, weight: .semibold))
                            Text("\(item.kindLabel) · \(item.subtitle)")
                                .font(.system(size: 12))
                                .foregroundStyle(.secondary)
                                .lineLimit(1)
                        }
                        Spacer()
                        Text(item.distanceLabel)
                            .font(.system(size: 12))
                            .foregroundStyle(.secondary)
                    }
                    .contentShape(Rectangle())
                    .onTapGesture { selectedPoiID = item.id }
                    .padding(.horizontal, 10)
                    .padding(.vertical, 8)
                    .background(Color.black.opacity(0.06), in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                }
            }
        }
    }

    @ViewBuilder
    private func detailView(item: SearchDockPoiViewItem) -> some View {
        detailHeader(title: item.title, onBack: { selectedPoiID = nil })

        Divider()

        VStack(alignment: .leading, spacing: 8) {
            detailRow(label: "Kind", value: item.kindLabel)
            detailRow(label: "Name", value: item.subtitle)
            if let frequency = item.frequency, !frequency.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                detailRow(label: "Frequency", value: frequency)
            }
            detailRow(label: "Distance", value: item.distanceLabel)
            detailRow(label: "Position", value: String(format: "%.4f, %.4f", item.latitude, item.longitude))
        }
        .padding(.horizontal, 4)

        VStack(spacing: 8) {
            detailActionButton(title: "Locate on Map", systemImage: "mappin.and.ellipse") { onPoiTap(item) }
            if !dock.isRouteDestination(item.id) {
                detailActionButton(title: "Add to Route", systemImage: "plus") { onAddToRouteTap(item) }
            }
        }
        .padding(.top, 4)
    }
}
