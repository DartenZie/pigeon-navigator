import SwiftUI
import Shared

struct ExpandableSearchPanel: View {
    let containerSize: CGSize
    let topReservedHeight: CGFloat
    let selectedLatitude: Double?
    let selectedLongitude: Double?
    let isLoading: Bool
    let errorMessage: String?
    let airports: [NearbyAirport]
    let airspaces: [Airspace]

    @State private var query: String = ""
    @State private var stage: ExpansionStage = .collapsed
    @GestureState private var handleDragTranslation: CGFloat = 0
    @FocusState private var isSearchFocused: Bool

    private enum ExpansionStage {
        case collapsed
        case partial
        case full
    }

    private var hasSelection: Bool {
        selectedLatitude != nil && selectedLongitude != nil
    }

    private var collapsedHeight: CGFloat { 64 }

    private var fullHeight: CGFloat {
        max(collapsedHeight, containerSize.height - topReservedHeight)
    }

    private var partialHeight: CGFloat {
        let preferredHalf = containerSize.height * 0.5
        let preferred = max(preferredHalf, collapsedHeight + 120)
        return min(fullHeight - 24, preferred)
    }

    private var baseHeight: CGFloat {
        switch stage {
        case .collapsed:
            return collapsedHeight
        case .partial:
            return partialHeight
        case .full:
            return fullHeight
        }
    }

    private var currentHeight: CGFloat {
        let dragged = baseHeight - handleDragTranslation
        return min(max(dragged, collapsedHeight), fullHeight)
    }

    private var horizontalInset: CGFloat {
        stage == .full ? 0 : 16
    }

    private var bottomInset: CGFloat {
        stage == .full ? 0 : 18
    }

    private var cornerRadius: CGFloat {
        stage == .full ? 0 : 32
    }

    private var coordinateLabel: String {
        guard let lat = selectedLatitude, let lon = selectedLongitude else {
            return "Search"
        }
        return String(format: "%.4f, %.4f", lat, lon)
    }

    private var handleDragGesture: some Gesture {
        DragGesture(minimumDistance: 4)
            .updating($handleDragTranslation) { value, state, _ in
                state = value.translation.height
            }
            .onEnded { value in
                let projectedHeight = min(
                    max(baseHeight - value.predictedEndTranslation.height, collapsedHeight),
                    fullHeight
                )
                let collapseThreshold = (collapsedHeight + partialHeight) * 0.5
                let fullThreshold = (partialHeight + fullHeight) * 0.5

                if projectedHeight >= fullThreshold {
                    setStage(.full)
                } else if projectedHeight >= collapseThreshold {
                    setStage(.partial)
                } else {
                    setStage(.collapsed)
                }
            }
    }

    private func setStage(_ newStage: ExpansionStage, animated: Bool = true) {
        if animated {
            withAnimation(.spring(response: 0.3, dampingFraction: 0.9)) {
                stage = newStage
            }
        } else {
            var transaction = Transaction()
            transaction.disablesAnimations = true
            withTransaction(transaction) {
                stage = newStage
            }
        }
    }

    var body: some View {
        VStack(spacing: 0) {
            ZStack {
                HStack(spacing: 8) {
                    Image(systemName: "magnifyingglass")
                        .foregroundStyle(.black)

                    TextField(coordinateLabel, text: $query)
                        .focused($isSearchFocused)
                        .autocorrectionDisabled(true)

                    if !query.isEmpty {
                        Button {
                            query = ""
                        } label: {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundStyle(.gray)
                        }
                        .buttonStyle(.plain)
                    }
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
                    .fill(.gray.opacity(0.55))
                    .frame(width: 34, height: 4)
                    .padding(.top, 4)
                    .contentShape(Rectangle())
                    .onTapGesture {
                        if hasSelection {
                            setStage(.partial)
                        }
                    }
                    .gesture(handleDragGesture)
            }

            if stage != .collapsed {
                Divider().padding(.horizontal, 10)
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 10) {
                        if isLoading {
                            Text("Fetching nearby data...")
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                        }

                        if let errorMessage {
                            Text(errorMessage)
                                .font(.footnote)
                                .foregroundStyle(.red)
                        }

                        Group {
                            Text("Airports")
                                .font(.caption)
                                .fontWeight(.semibold)

                            if airports.isEmpty {
                                Text("No airports near this point")
                                    .font(.footnote)
                                    .foregroundStyle(.secondary)
                            } else {
                                ForEach(airports, id: \.airport.id) { airport in
                                    AirportRow(airport: airport)
                                }
                            }
                        }

                        Group {
                            Text("Airspaces")
                                .font(.caption)
                                .fontWeight(.semibold)
                                .padding(.top, 4)

                            if airspaces.isEmpty {
                                Text("No airspaces contain this point")
                                    .font(.footnote)
                                    .foregroundStyle(.secondary)
                            } else {
                                ForEach(airspaces, id: \.id) { airspace in
                                    AirspaceRow(airspace: airspace)
                                }
                            }
                        }
                    }
                    .padding(.horizontal, 18)
                    .padding(.top, 10)
                    .padding(.bottom, 18)
                }
                .simultaneousGesture(
                    DragGesture(minimumDistance: 6)
                        .onChanged { _ in
                            if stage == .partial {
                                setStage(.full)
                            }
                        }
                )
            }
        }
        .frame(maxWidth: .infinity, alignment: .top)
        .frame(height: currentHeight, alignment: .top)
        .background(backgroundShape)
        .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
        .padding(.horizontal, horizontalInset)
        .padding(.bottom, bottomInset)
        .animation(.spring(response: 0.3, dampingFraction: 0.9), value: stage)
        .animation(.spring(response: 0.3, dampingFraction: 0.9), value: handleDragTranslation)
        .onChange(of: isSearchFocused) { focused in
            guard focused else { return }
            setStage(.full, animated: false)
        }
        .onChange(of: hasSelection) { value in
            if value {
                setStage(.partial)
            } else {
                setStage(.collapsed)
            }
        }
    }

    @ViewBuilder
    private var backgroundShape: some View {
        if stage == .full {
            Color.white
        } else {
            RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                .modifier(HUDSearchBarGlassStyle())
        }
    }
}

private struct AirportRow: View {
    let airport: NearbyAirport

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: "location.fill")
                .font(.caption)
                .foregroundStyle(.blue)

            VStack(alignment: .leading, spacing: 2) {
                Text(airport.airport.id)
                    .font(.subheadline)
                    .fontWeight(.semibold)
                Text(airport.airport.name)
                    .font(.footnote)
                    .lineLimit(1)
                    .foregroundStyle(.secondary)
            }

            Spacer(minLength: 0)

            Text(formatDistance(airport.distanceMeters))
                .font(.caption)
                .foregroundStyle(.secondary)
        }
    }
}

private struct AirspaceRow: View {
    let airspace: Airspace

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(airspace.name.isEmpty ? airspace.id : airspace.name)
                .font(.subheadline)
                .fontWeight(.semibold)
            Text("\(airspace.kind) · \(formatAltitudeBand(airspace))")
                .font(.footnote)
                .foregroundStyle(.secondary)
        }
    }
}

private func formatDistance(_ distanceMeters: Double) -> String {
    if distanceMeters >= 1000 {
        return String(format: "%.1f km", distanceMeters / 1000.0)
    }
    return "\(Int(distanceMeters.rounded())) m"
}

private func formatAltitudeBand(_ airspace: Airspace) -> String {
    let lower = {
        if let meters = airspace.lowerLimitMeters {
            return "\(meters)m \(airspace.lowerLimitReference ?? "")".trimmingCharacters(in: .whitespaces)
        }
        return "SFC"
    }()

    let upper = {
        if let meters = airspace.upperLimitMeters {
            return "\(meters)m \(airspace.upperLimitReference ?? "")".trimmingCharacters(in: .whitespaces)
        }
        return "UNL"
    }()

    return "\(lower) - \(upper)"
}

private struct HUDSearchBarGlassStyle: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 26.0, *) {
            content
                .glassEffect(
                    .regular,
                    in: RoundedRectangle(cornerRadius: 32, style: .continuous)
                )
        } else {
            content
                .background(
                    .ultraThinMaterial,
                    in: RoundedRectangle(cornerRadius: 32, style: .continuous)
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 32, style: .continuous)
                        .stroke(.white.opacity(0.25), lineWidth: 1)
                )
        }
    }
}
