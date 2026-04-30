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
    var onNearbyPoiTap: (SearchDockPoiViewItem) -> Void = { _ in }
    var onSearchResultTap: (SearchDockResultViewItem) -> Void = { _ in }
    var onMapTapAirportTap: (MapTapAirportItem) -> Void = { _ in }
    var onMapTapAirspaceTap: (MapTapAirspaceItem) -> Void = { _ in }
    var onMapTapNavaidTap: (MapTapNavaidItem) -> Void = { _ in }
    var onAddSearchResultToRouteTap: (SearchDockResultViewItem) -> Void = { _ in }
    var onAddNearbyPoiToRouteTap: (SearchDockPoiViewItem) -> Void = { _ in }
    var onAddMapTapToRouteTap: (String, String, Double, Double) -> Void = { _, _, _, _ in }
    var minimumSearchQueryLength: Int = 2
    var searchDebounceDelay: TimeInterval = 0.3

    private var size: HUDSize { hudSize }
    @GestureState private var dragOffset: CGFloat = 0
    @FocusState private var isSearchFocused: Bool
    @State private var pendingSearchWorkItem: DispatchWorkItem? = nil
    private let handleTopPadding: CGFloat = 4
    private let handleWidth: CGFloat = 36
    private let handleHeight: CGFloat = 5
    private var actionRowHeight: CGFloat {
        dock.headerMode == .navigation ? 60 : 48
    }

    var body: some View {
        GeometryReader { geo in
            VStack(spacing: 0) {
                Spacer(minLength: 0)

                ZStack(alignment: .top) {
                    searchRow(geo: geo)
                    dragHandle

                    if size == .half || size == .full {
                        resultsContent
                            .padding(.top, expandedContentTopPadding)
                            .transition(.opacity.combined(with: .move(edge: .top)))
                    }
                }
                .frame(
                    width: actionBarWidth(in: geo),
                    height: actionBarHeight(in: geo) + dragResistance(dragOffset)
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
            if newSize == .full && dock.activeRoute == .navigationDetail {
                hudSize = .half
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
        Group {
            if dock.headerMode == .navigation {
                navigationSummaryRow
            } else {
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
            }
        }
        .padding(.horizontal, 10)
        .background(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(size == .bar || dock.headerMode == .navigation ? Color.clear : Color.white)
        )
        .padding(.horizontal, 14)
        .padding(.vertical, 6)
        .frame(width: actionBarWidth(in: geo), height: actionRowHeight)
        .offset(y: size == .bar ? 0 : 6)
        .contentShape(Rectangle())
        .onTapGesture {
            if dock.headerMode == .navigation {
                withAnimation(.spring(response: 0.44, dampingFraction: 0.76)) {
                    hudSize = .half
                }
                dock.openNavigationDetail()
                return
            }

            guard size != .full else { return }

            withAnimation(.spring(response: 0.44, dampingFraction: 0.76)) {
                hudSize = .full
            }
            DispatchQueue.main.async {
                isSearchFocused = true
            }
        }
    }

    private var navigationSummaryRow: some View {
        HStack(spacing: size == .bar ? 42 : 12) {
            if size != .bar { Spacer(minLength: 0) }
            navigationMetric(value: arrivalLabel, label: "arrival")
            if size != .bar { Spacer(minLength: 0) }
            navigationMetric(value: remainingMinutesLabel, label: "min")
            if size != .bar { Spacer(minLength: 0) }
            navigationMetric(value: remainingDistanceValue, label: remainingDistanceUnit)
            if size != .bar { Spacer(minLength: 0) }
        }
        .frame(maxWidth: .infinity)
    }

    private func navigationMetric(value: String, label: String) -> some View {
        VStack(spacing: 2) {
            Text(value)
                .font(.system(size: size == .bar ? 19 : 21, weight: .bold))
                .foregroundStyle(.black)
            Text(label)
                .font(.system(size: size == .bar ? 11 : 13, weight: .semibold))
                .foregroundStyle(.black.opacity(0.6))
        }
    }

    private func actionBarWidth(in geo: GeometryProxy) -> CGFloat {
        if dock.headerMode == .navigation && size == .bar {
            return min(geo.size.width - 64, 340)
        }
        return size.width(in: geo)
    }

    private func actionBarHeight(in geo: GeometryProxy) -> CGFloat {
        if dock.headerMode == .navigation && size == .bar {
            return 60
        }
        if dock.activeRoute == .navigationDetail && size == .half {
            return navigationDetailHalfHeight
        }
        return size.height(in: geo)
    }

    private var navigationDetailHalfHeight: CGFloat {
        if dock.selectedNavigationWaypoint != nil {
            let detailContentHeight: CGFloat = 132
            let contentPaddingTop: CGFloat = expandedContentTopPadding
            let routeContentBottomPadding: CGFloat = 16
            return contentPaddingTop + detailContentHeight + routeContentBottomPadding
        }

        let buttonCount: CGFloat = dock.nextWaypoint != nil ? 3 : 2
        let buttonHeight: CGFloat = 58
        let buttonSpacing: CGFloat = 12
        let bottomPadding: CGFloat = 16
        let contentPaddingTop: CGFloat = expandedContentTopPadding
        let routeContentBottomPadding: CGFloat = 16
        let verticalPadding: CGFloat = 0
        let buttonsHeight = buttonCount * buttonHeight + max(0, buttonCount - 1) * buttonSpacing + bottomPadding
        return contentPaddingTop + buttonsHeight + routeContentBottomPadding + verticalPadding
    }

    private var expandedContentTopPadding: CGFloat {
        dock.activeRoute == .navigationDetail ? 78 : 52
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
        .simultaneousGesture(
            DragGesture(minimumDistance: 1)
                .onChanged { _ in
                    guard hudSize == .half else { return }
                    guard dock.activeRoute != .navigationDetail else { return }
                    withAnimation(.spring(response: 0.44, dampingFraction: 0.76)) {
                        hudSize = .full
                    }
                }
        )
    }

    @ViewBuilder
    private var routeContent: some View {
        switch dock.activeRoute {
        case .search:
            HUDSearchSearchPage(
                dock: dock,
                minimumQueryLength: minimumSearchQueryLength,
                onResultTap: onSearchResultTap,
                onAddToRouteTap: onAddSearchResultToRouteTap
            )
        case .routePlanner:
            HUDSearchRoutePlannerPage(dock: dock)
        case .mapTap:
            HUDSearchMapTapPage(
                mapTapLookup: mapTapLookup,
                dock: dock,
                onAirportTap: onMapTapAirportTap,
                onAirspaceTap: onMapTapAirspaceTap,
                onNavaidTap: onMapTapNavaidTap,
                onAddToRouteTap: onAddMapTapToRouteTap
            )
        case .nearby:
            HUDSearchNearbyPage(
                dock: dock,
                onPoiTap: onNearbyPoiTap,
                onAddToRouteTap: onAddNearbyPoiToRouteTap
            )
        case .navigationDetail:
            HUDNavigationDetailPage(dock: dock)
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
        let projectedMotion = velocity - drag
        if dock.activeRoute == .navigationDetail && projectedMotion < -500 { return .half }
        if projectedMotion < -500 { return .full }
        if projectedMotion > 500 { return .bar }
        if abs(drag) > 24 { return .half }

        let projected = size.height(in: geo) - drag
        return [HUDSize.bar, .half, .full]
            .min(by: { abs($0.height(in: geo) - projected) < abs($1.height(in: geo) - projected) })!
    }

    private func expandedSize(from s: HUDSize) -> HUDSize {
        switch s {
        case .bar:
            return .half
        case .half:
            return dock.activeRoute == .navigationDetail ? .half : .full
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

    private var arrivalLabel: String {
        guard let seconds = dock.navigationSummary?.remainingSeconds?.int64Value else { return "--:--" }
        let arrival = Date().addingTimeInterval(TimeInterval(seconds))
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm"
        return formatter.string(from: arrival)
    }

    private var remainingMinutesLabel: String {
        guard let seconds = dock.navigationSummary?.remainingSeconds?.int64Value else { return "--" }
        return String(max(1, Int((Double(seconds) / 60.0).rounded())))
    }

    private var remainingDistanceUnit: String {
        guard let meters = dock.navigationSummary?.remainingDistanceMeters else { return "km" }
        return meters >= 1000 ? "km" : "m"
    }

    private var remainingDistanceValue: String {
        guard let meters = dock.navigationSummary?.remainingDistanceMeters else { return "--" }
        if meters >= 1000 {
            return String(format: "%.1f", meters / 1000.0)
        }
        return String(Int(meters.rounded()))
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
