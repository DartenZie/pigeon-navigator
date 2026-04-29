import SwiftUI
import CoreLocation

struct CompassBubble: View {
    let direction: CLLocationDirection
    let size: CGFloat
    let action: () -> Void

    private var normalizedDirection: CLLocationDirection {
        let value = direction.truncatingRemainder(dividingBy: 360)
        return value >= 0 ? value : value + 360
    }

    var body: some View {
        Button(action: action) {
            Image(systemName: "safari.fill")
                .font(.system(size: size * 0.36, weight: .semibold))
                .foregroundStyle(.primary)
                .frame(width: size, height: size)
                .rotationEffect(.degrees(-normalizedDirection - 45))
        }
        .buttonStyle(.plain)
        .contentShape(Circle())
        .modifier(CompassBubbleGlassStyle())
    }
}

private struct CompassBubbleGlassStyle: ViewModifier {
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
