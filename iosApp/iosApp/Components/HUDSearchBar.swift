import SwiftUI
import Shared

enum HUDSize {
    case bar, half, full
    
    func width(in geo: GeometryProxy) -> CGFloat {
        switch self {
        case .bar: return 220
        case .half: return geo.size.width - 32
        case .full: return geo.size.width
        }
    }
    
    func height(in geo: GeometryProxy) -> CGFloat {
        switch self {
        case .bar: return 44
        case .half: return 320
        case .full:
            let bottomSafeArea = geo.safeAreaInsets.bottom
            return geo.size.height + bottomSafeArea - 76
        }
    }
    
    var cornerRadius: CGFloat {
        switch self {
        case .bar: return 32
        case .half: return 24
        case .full: return 16
        }
    }
    
    func yOffset(in geo: GeometryProxy) -> CGFloat {
        switch self {
        case .bar, .half:
            return 0
        case .full:
            return geo.safeAreaInsets.bottom
        }
    }
}

struct HUDSearchBar: View {
    @Binding var hudSize: HUDSize
    private var size: HUDSize { hudSize }
    @GestureState private var dragOffset: CGFloat = 0
    private let handleTopPadding: CGFloat = 8
    private let handleWidth: CGFloat = 36
    private let handleHeight: CGFloat = 5
    private let searchRowHeight: CGFloat = 48
    
    var body: some View {
        GeometryReader { geo in
            VStack(spacing: 0) {
                Spacer(minLength: 0)

                ZStack(alignment: .top) {
                    searchRow(geo: geo)
                    dragHandle

                    if size == .half || size == .full {
                        resultsContent
                            .padding(.top, 52)
                            .transition(.opacity.combined(with: .move(edge: .top)))
                    }
                }
                .frame(
                    width: size.width(in: geo),
                    height: size.height(in: geo) + dragResistance(dragOffset)
                )
                .modifier(HUDSearchBarGlassStyle(cornerRadius: size.cornerRadius, isSolid: size == .full))
                .clipShape(RoundedRectangle(cornerRadius: size.cornerRadius, style: .continuous))
                .offset(y: size.yOffset(in: geo))
                .gesture(dragGesture(geo: geo))
                .animation(.spring(response: 0.44, dampingFraction: 0.76), value: size)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottom)
        }
    }
    
    // MARK: Sub-views

    private var dragHandle: some View {
        Button {
            withAnimation(.spring(response: 0.44, dampingFraction: 0.76)) {
                hudSize = expandedSize(from: size)
            }
        } label: {
            Capsule(style: .continuous)
                .fill(.black.opacity(size == .full ? 0.18 : 0.24))
                .frame(width: handleWidth, height: handleHeight)
                .frame(maxWidth: .infinity)
                .padding(.top, handleTopPadding)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
    
    private func searchRow(geo: GeometryProxy) -> some View {
        HStack(spacing: 8) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(.black.opacity(0.8))
            
            Text("Search...")
                .font(.system(size: 14))
                .foregroundStyle(.black.opacity(0.75))
                .lineLimit(1)
            
            Spacer()
        }
        .padding(.horizontal, 14)
        .frame(width: size.width(in: geo), height: searchRowHeight)
        .contentShape(Rectangle())
        .onTapGesture {
            withAnimation(.spring(response: 0.44, dampingFraction: 0.76)) {
                hudSize = (size == .bar) ? .half : size
            }
        }
    }
    
    private var resultsContent: some View {
        ScrollView {
            VStack(spacing: 0) {
                // TODO: result rows here
            }
        }
    }
    
    // MARK: - Drag
    
    private func dragGesture(geo: GeometryProxy) -> some Gesture {
        DragGesture()
            .updating($dragOffset) { value, state, _ in
                state = value.translation.height
            }
            .onEnded { value in
                let velocity = value.predictedEndTranslation.height
                withAnimation(.spring(response: 0.44, dampingFraction: 0.76)) {
                    hudSize = nextSize(velocity: velocity,
                                    drag: value.translation.height,
                                    geo: geo)
                }
            }
    }
    
    private func nextSize(velocity: CGFloat, drag: CGFloat, geo: GeometryProxy) -> HUDSize {
        // Fast flick takes priority over position
        if velocity < -500 { return expandedSize(from: size) }
        if velocity > 500 { return collapsedSize(from: size) }
        
        // Slow drag - pick nearest by distance
        let projected = size.height(in: geo) - drag
        return [HUDSize.bar, .half, .full]
            .min(by: { abs($0.height(in: geo) - projected) < abs($1.height(in: geo) - projected) })!
    }
    
    private func expandedSize(from s: HUDSize) -> HUDSize {
        switch s { case .bar: return .half; case .half: return .full; case .full: return .full }
    }
    private func collapsedSize(from s: HUDSize) -> HUDSize {
        switch s { case .full: return .half;  case .half: return .bar case .bar: return .bar }
    }
    
    // Rubber-band resistance past limits
    private func dragResistance(_ dy: CGFloat) -> CGFloat {
        guard dy != 0 else { return 0 }
        return -dy * 0.25
    }
}

private struct HUDSearchBarGlassStyle: ViewModifier {
    let cornerRadius: CGFloat
    let isSolid: Bool
    
    func body(content: Content) -> some View {
        if isSolid {
            content
                .background(
                    Color.white,
                    in: RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                )
        } else if #available(iOS 26.0, *) {
            content
                .glassEffect(
                    .regular,
                    in: RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                )
        } else {
            content
                .background(
                    .ultraThinMaterial,
                    in: RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                )
                .overlay(
                    RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                        .stroke(.white.opacity(0.25), lineWidth: 1)
                )
        }
    }
}
