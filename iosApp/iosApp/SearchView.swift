import SwiftUI

struct SearchView: View {
    @StateObject private var viewModel = SearchViewModelWrapper()

    var body: some View {
        NavigationStack {
            VStack(spacing: 12) {
                HStack(spacing: 8) {
                    TextField("Search places", text: $viewModel.query)
                        .textFieldStyle(.roundedBorder)
                        .onChange(of: viewModel.query) { (newValue: String) in
                            viewModel.onQueryChange(newValue)
                            if newValue.isEmpty {
                                viewModel.clearSearch()
                            } else {
                                viewModel.submitSearch()
                            }
                        }
                        .submitLabel(.search)
                        .onSubmit {
                            viewModel.submitSearch()
                        }

                    Button("Search") {
                        viewModel.submitSearch()
                    }
                    Button("Clear") {
                        viewModel.clearSearch()
                    }
                }

                if viewModel.isLoading {
                    ProgressView()
                }

                if let errorMessage = viewModel.errorMessage {
                    Text(errorMessage)
                        .font(.footnote)
                        .foregroundStyle(.red)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }

                List(viewModel.results, id: \.self) { item in
                    Text(item)
                }
                .listStyle(.plain)
            }
            .padding()
            .navigationTitle("Search")
        }
    }
}
