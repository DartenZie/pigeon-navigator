import SwiftUI
import Shared

struct HUDSearchNearbyPage: View {
    @ObservedObject var dock: SearchDockViewModelWrapper
    var onPoiTap: (SearchDockPoiViewItem) -> Void = { _ in }
    var onAddToRouteTap: () -> Void = {}

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
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
                            Button(action: onAddToRouteTap) {
                                Image(systemName: "plus")
                                    .font(.system(size: 13, weight: .semibold))
                            }
                            .buttonStyle(.plain)
                        }
                        .contentShape(Rectangle())
                        .onTapGesture { onPoiTap(item) }
                        .padding(.horizontal, 10)
                        .padding(.vertical, 8)
                        .background(Color.black.opacity(0.06), in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                    }
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}
