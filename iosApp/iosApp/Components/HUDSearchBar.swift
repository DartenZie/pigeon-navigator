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

    var isExpanded: Bool {
        self != .bar
    }
}

struct HUDSearchBar: View {
    @Binding var hudSize: HUDSize
    @ObservedObject var dock: SearchDockViewModelWrapper
    @ObservedObject var mapTapLookup: MapTapLookupViewModelWrapper

    private var size: HUDSize { hudSize }
    @GestureState private var dragOffset: CGFloat = 0
    @FocusState private var isSearchFocused: Bool
    @State private var pendingSearchWorkItem: DispatchWorkItem? = nil
    private let handleTopPadding: CGFloat = 4
    private let handleWidth: CGFloat = 36
    private let handleHeight: CGFloat = 5
    private let searchRowHeight: CGFloat = 48
    private let minimumSearchQueryLength = 2
    private let searchDebounceDelay: TimeInterval = 0.35

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
        .onChange(of: size.isExpanded) { (expanded: Bool) in
            dock.onExpandedChanged(expanded)
        }
        .onChange(of: size) { (newSize: HUDSize) in
            if newSize != .full {
                isSearchFocused = false
            }
        }
        .onChange(of: isSearchFocused) { (focused: Bool) in
            if focused {
                dock.selectRoute(.search)
            } else {
                cancelPendingSearch()
            }
        }
        .onDisappear {
            cancelPendingSearch()
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

            TextField("Search...", text: $dock.query)
                .font(.system(size: 14))
                .foregroundStyle(.black)
                .padding(.vertical, 6)
                .padding(.horizontal, 2)
                .autocorrectionDisabled()
                .submitLabel(.search)
                .focused($isSearchFocused)
                .allowsHitTesting(size == .full)
                .onSubmit {
                    dock.submitSearch()
                }
                .onChange(of: dock.query) { (newQuery: String) in
                    dock.onQueryChange(newQuery)
                    cancelPendingSearch()

                    let trimmed = newQuery.trimmingCharacters(in: .whitespacesAndNewlines)
                    if trimmed.isEmpty {
                        dock.clearSearch()
                        if isSearchFocused {
                            dock.selectRoute(.search)
                        }
                        return
                    }

                    guard isSearchFocused else { return }

                    dock.selectRoute(.search)
                    guard trimmed.count >= minimumSearchQueryLength else { return }

                    let workItem = DispatchWorkItem {
                        dock.submitSearch()
                    }
                    pendingSearchWorkItem = workItem
                    DispatchQueue.main.asyncAfter(deadline: .now() + searchDebounceDelay, execute: workItem)
                }

            Spacer()
        }
        .padding(.horizontal, 10)
        .background(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(size == .bar ? Color.clear : Color.white)
        )
        .padding(.horizontal, 14)
        .padding(.vertical, 6)
        .frame(width: size.width(in: geo), height: searchRowHeight)
        .offset(y: size == .bar ? 0 : 6)
        .contentShape(Rectangle())
        .onTapGesture {
            guard size != .full else { return }

            withAnimation(.spring(response: 0.44, dampingFraction: 0.76)) {
                hudSize = .full
            }
            DispatchQueue.main.async {
                isSearchFocused = true
            }
        }
    }

    private func cancelPendingSearch() {
        pendingSearchWorkItem?.cancel()
        pendingSearchWorkItem = nil
    }

    private var resultsContent: some View {
        ScrollView {
            VStack(spacing: 12) {
                routeContent
            }
            .padding(.horizontal, 12)
            .padding(.bottom, 16)
        }
    }

    @ViewBuilder
    private var routeContent: some View {
        switch dock.activeRoute {
        case .search:
            HUDSearchSearchPage(dock: dock)
        case .routePlanner:
            HUDSearchRoutePlannerPage()
        case .mapTap:
            HUDSearchMapTapPage(mapTapLookup: mapTapLookup)
        case .nearby:
            HUDSearchNearbyPage(dock: dock)
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
        if velocity < -500 { return expandedSize(from: size) }
        if velocity > 500 { return collapsedSize(from: size) }

        let projected = size.height(in: geo) - drag
        return [HUDSize.bar, .half, .full]
            .min(by: { abs($0.height(in: geo) - projected) < abs($1.height(in: geo) - projected) })!
    }

    private func expandedSize(from s: HUDSize) -> HUDSize {
        switch s {
        case .bar:
            return .half
        case .half:
            return .full
        case .full:
            return .full
        }
    }

    private func collapsedSize(from s: HUDSize) -> HUDSize {
        switch s {
        case .full:
            return .half
        case .half:
            return .bar
        case .bar:
            return .bar
        }
    }

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
