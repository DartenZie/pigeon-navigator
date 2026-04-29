import SwiftUI

/// Standalone search screen.
///
/// Wires `SearchViewModelWrapper` (which owns the shared `SearchStore`) into
/// `SearchContent`. The screen mirrors the previous `SearchView` behaviour:
/// auto-submits on each non-empty query change, clears on empty input, and
/// resubmits on the keyboard `search` action.
struct SearchScreen: View {
    @StateObject private var viewModel = SearchViewModelWrapper()

    var body: some View {
        NavigationStack {
            SearchContent(
                query: $viewModel.query,
                isLoading: viewModel.isLoading,
                errorMessage: viewModel.errorMessage,
                results: viewModel.results,
                onQueryChange: { newValue in
                    viewModel.onQueryChange(newValue)
                    if newValue.isEmpty {
                        viewModel.clearSearch()
                    } else {
                        viewModel.submitSearch()
                    }
                },
                onSubmit: { viewModel.submitSearch() },
                onClear: { viewModel.clearSearch() }
            )
            .navigationTitle("Search")
        }
    }
}
