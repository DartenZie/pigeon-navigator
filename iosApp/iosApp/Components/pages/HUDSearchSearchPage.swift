import SwiftUI

struct HUDSearchSearchPage: View {
    @ObservedObject var dock: SearchDockViewModelWrapper

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 8) {
                Button("Find") {
                    dock.submitSearch()
                }
                .buttonStyle(.borderedProminent)

                Button("Clear") {
                    dock.clearSearch()
                }
                .buttonStyle(.bordered)
            }

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
                Text("No search results")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
            } else {
                LazyVStack(spacing: 8) {
                    ForEach(dock.results, id: \.self) { item in
                        HStack(spacing: 8) {
                            Image(systemName: "mappin")
                                .font(.system(size: 12, weight: .semibold))
                                .foregroundStyle(.secondary)
                            Text(item)
                                .font(.system(size: 14, weight: .medium))
                            Spacer()
                        }
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
