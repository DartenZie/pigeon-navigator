import SwiftUI

/// Stateless layout for the standalone search screen.
///
/// Receives strings/booleans only and forwards user input through callbacks;
/// the owning `SearchScreen` is responsible for store wiring.
struct SearchContent: View {
    @Binding var query: String
    let isLoading: Bool
    let errorMessage: String?
    let results: [String]
    let onQueryChange: (String) -> Void
    let onSubmit: () -> Void
    let onClear: () -> Void

    var body: some View {
        VStack(spacing: 12) {
            HStack(spacing: 8) {
                TextField("Search places", text: $query)
                    .textFieldStyle(.roundedBorder)
                    .onChange(of: query) { newValue in
                        onQueryChange(newValue)
                    }
                    .submitLabel(.search)
                    .onSubmit { onSubmit() }

                Button("Search") { onSubmit() }
                Button("Clear") { onClear() }
            }

            if isLoading {
                ProgressView()
            }

            if let errorMessage = errorMessage {
                Text(errorMessage)
                    .font(.footnote)
                    .foregroundStyle(.red)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }

            List(results, id: \.self) { item in
                Text(item)
            }
            .listStyle(.plain)
        }
        .padding()
    }
}
