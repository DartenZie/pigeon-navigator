import SwiftUI
import Shared

struct HUDSearchSearchPage: View {
    @ObservedObject var dock: SearchDockViewModelWrapper
    var onResultTap: (SearchDockResultViewItem) -> Void = { _ in }
    var onAddToRouteTap: () -> Void = {}

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
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
                if dock.query.trimmingCharacters(in: .whitespacesAndNewlines).count >= 2 && !dock.isSearching {
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
                            Button(action: onAddToRouteTap) {
                                Image(systemName: "plus")
                                    .font(.system(size: 13, weight: .semibold))
                            }
                            .buttonStyle(.plain)
                        }
                        .contentShape(Rectangle())
                        .onTapGesture { onResultTap(item) }
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
