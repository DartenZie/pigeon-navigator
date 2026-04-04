import SwiftUI

struct HUDSearchBar: View {
    var body: some View {
        ZStack {
            HStack(spacing: 8) {
                Image(systemName: "magnifyingglass")
                    .foregroundStyle(.black)
                Text("Search")
                    .foregroundStyle(.gray)
                Spacer(minLength: 0)
            }
            .padding(.horizontal, 12)
            .frame(height: 38)
            .background(
                Capsule()
                    .fill(.white)
            )
        }
        .frame(height: 64)
        .padding(.horizontal, 12)
        .frame(maxWidth: 332)
        .overlay(alignment: .top) {
            Capsule()
                .fill(.gray)
                .frame(width: 34, height: 4)
                .padding(.top, 4)
        }
        .modifier(HUDSearchBarGlassStyle())
    }
}

private struct HUDSearchBarGlassStyle: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 26.0, *) {
            content
                .glassEffect(.regular, in: Capsule())
        } else {
            content
                .background(.ultraThinMaterial, in: Capsule())
                .overlay(
                    Capsule().stroke(.white.opacity(0.25), lineWidth: 1)
                )
        }
    }
}
