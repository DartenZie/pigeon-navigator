import SwiftUI

/// Edits the time-to-collision threshold (5–600 seconds).
///
/// Owns its own input draft so non-numeric characters can be filtered out
/// before being committed back through `onCommitSeconds`.
struct WarningSection: View {
    @Binding var draft: String
    let onCommitSeconds: (Int32) -> Void

    var body: some View {
        Section(header: Text("Terrain warning")) {
            TextField("Seconds", text: $draft)
                .keyboardType(.numberPad)
                .onChange(of: draft) { newValue in
                    commitTimeToCollision(newValue)
                }
            Text("5-600 seconds")
                .font(.footnote)
                .foregroundStyle(.secondary)
        }
    }

    private func commitTimeToCollision(_ value: String) {
        let digitsOnly = value.filter(\.isNumber)
        if digitsOnly != value {
            draft = digitsOnly
            return
        }
        guard let seconds = Int32(digitsOnly), (5...600).contains(seconds) else { return }
        onCommitSeconds(seconds)
    }
}
