import SwiftUI
import Shared

struct HUDSearchSearchPage: View {
    @ObservedObject var dock: SearchDockViewModelWrapper
    var minimumQueryLength: Int = 2
    var onResultTap: (SearchDockResultViewItem) -> Void = { _ in }
    var onAddToRouteTap: (SearchDockResultViewItem) -> Void = { _ in }

    @State private var selectedResultID: String? = nil

    private var selectedResult: SearchDockResultViewItem? {
        guard let selectedResultID else { return nil }
        return dock.results.first { $0.id == selectedResultID }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            if let selectedResult {
                detailView(item: selectedResult)
            } else {
                listView
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .onReceive(dock.$results) { results in
            if let selectedResultID, !results.contains(where: { $0.id == selectedResultID }) {
                self.selectedResultID = nil
            }
        }
    }

    @ViewBuilder
    private var listView: some View {
        if dock.isSearching {
            ProgressView()
                .frame(maxWidth: .infinity, alignment: .leading)
        }

        if let errorMessage = dock.errorMessage {
            Text(errorMessage)
                .font(.footnote)
                .foregroundStyle(.red)
                .frame(maxWidth: .infinity, alignment: .leading)
        }

        if dock.results.isEmpty {
            if dock.query.trimmingCharacters(in: .whitespacesAndNewlines).count >= minimumQueryLength && !dock.isSearching {
                Text("No search results")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        } else {
            LazyVStack(spacing: 8) {
                ForEach(dock.results, id: \.id) { item in
                    HStack(spacing: 8) {
                        Image(systemName: "mappin")
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundStyle(.secondary)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(item.title)
                                .font(.system(size: 14, weight: .medium))
                            Text("\(item.kindLabel) · \(item.subtitle)")
                                .font(.system(size: 12))
                                .foregroundStyle(.secondary)
                                .lineLimit(1)
                        }
                        Spacer()
                    }
                    .contentShape(Rectangle())
                    .onTapGesture { selectedResultID = item.id }
                    .padding(.horizontal, 10)
                    .padding(.vertical, 8)
                    .background(Color.black.opacity(0.06), in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                }
            }
        }
    }

    @ViewBuilder
    private func detailView(item: SearchDockResultViewItem) -> some View {
        detailHeader(title: item.title, onBack: { selectedResultID = nil })

        Divider()

        VStack(alignment: .leading, spacing: 8) {
            detailRow(label: "Kind", value: item.kindLabel)
            detailRow(label: "Name", value: item.subtitle)
            if let frequency = item.frequency, !frequency.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                detailRow(label: "Frequency", value: frequency)
            }
            detailRow(label: "Position", value: String(format: "%.4f, %.4f", item.latitude, item.longitude))

            if let minLatitude = item.minLatitude,
               let minLongitude = item.minLongitude,
               let maxLatitude = item.maxLatitude,
               let maxLongitude = item.maxLongitude {
                detailRow(label: "Bounds NE", value: String(format: "%.4f, %.4f", maxLatitude, maxLongitude))
                detailRow(label: "Bounds SW", value: String(format: "%.4f, %.4f", minLatitude, minLongitude))
            }
        }
        .padding(.horizontal, 4)

        VStack(spacing: 8) {
            detailActionButton(title: "Locate on Map", systemImage: "mappin.and.ellipse") { onResultTap(item) }
            if !dock.isRouteDestination(item.id) {
                detailActionButton(title: "Add to Route", systemImage: "plus") { onAddToRouteTap(item) }
            }
        }
        .padding(.top, 4)
    }
}
