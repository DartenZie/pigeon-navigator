import SwiftUI

struct RecenterBubble: View {
    let size: CGFloat
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: "location.circle.fill")
                .font(.system(size: size * 0.34, weight: .semibold))
                .foregroundStyle(.primary)
                .frame(width: size, height: size)
        }
        .buttonStyle(.plain)
        .contentShape(Circle())
        .modifier(RecenterBubbleGlassStyle())
    }
}

private struct RecenterBubbleGlassStyle: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 26.0, *) {
            content
                .glassEffect(.regular, in: Circle())
        } else {
            content
                .background(.ultraThinMaterial, in: Circle())
                .overlay(Circle().stroke(.white.opacity(0.25), lineWidth: 1))
        }
    }
}
