import SwiftUI
import Shared

struct HUDSearchMapTapPage: View {
    @ObservedObject var mapTapLookup: MapTapLookupViewModelWrapper
    var onAirportTap: (MapTapAirportItem) -> Void = { _ in }
    var onAirspaceTap: (MapTapAirspaceItem) -> Void = { _ in }
    var onAddToRouteTap: () -> Void = {}

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Map Tap Details")
                .font(.system(size: 15, weight: .semibold))

            if let lat = mapTapLookup.selectedLatitude, let lon = mapTapLookup.selectedLongitude {
                Text(String(format: "%.4f, %.4f", lat, lon))
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            } else {
                Text("Tap on the map to inspect this view.")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }

            if mapTapLookup.isLoading {
                ProgressView("Fetching nearby data...")
                    .frame(maxWidth: .infinity, alignment: .leading)
            }

            if let errorMessage = mapTapLookup.errorMessage {
                Text(errorMessage)
                    .font(.footnote)
                    .foregroundStyle(.red)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }

            Group {
                Text("Airports")
                    .font(.system(size: 13, weight: .semibold))

                if mapTapLookup.airports.isEmpty {
                    Text("No airports near this point")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                } else {
                    LazyVStack(spacing: 8) {
                        ForEach(mapTapLookup.airports, id: \.id) { airport in
                            HStack(spacing: 8) {
                                Image(systemName: "airplane")
                                    .font(.system(size: 12, weight: .semibold))
                                    .foregroundStyle(.secondary)
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(airport.id)
                                        .font(.system(size: 13, weight: .semibold))
                                    Text(airport.name)
                                        .font(.system(size: 12))
                                        .foregroundStyle(.secondary)
                                }
                                Spacer()
                                Text(airport.distanceLabel)
                                    .font(.system(size: 12))
                                    .foregroundStyle(.secondary)
                                Button(action: onAddToRouteTap) {
                                    Image(systemName: "plus")
                                        .font(.system(size: 13, weight: .semibold))
                                }
                                .buttonStyle(.plain)
                            }
                            .contentShape(Rectangle())
                            .onTapGesture { onAirportTap(airport) }
                            .padding(.horizontal, 10)
                            .padding(.vertical, 8)
                            .background(Color.black.opacity(0.06), in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                        }
                    }
                }
            }

            Group {
                Text("Airspaces")
                    .font(.system(size: 13, weight: .semibold))

                if mapTapLookup.airspaces.isEmpty {
                    Text("No airspaces contain this point")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                } else {
                    LazyVStack(spacing: 8) {
                        ForEach(mapTapLookup.airspaces, id: \.id) { airspace in
                            HStack(spacing: 8) {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(airspace.name)
                                        .font(.system(size: 13, weight: .semibold))
                                    Text(airspace.detail)
                                        .font(.system(size: 12))
                                        .foregroundStyle(.secondary)
                                }
                                Spacer()
                                Button(action: onAddToRouteTap) {
                                    Image(systemName: "plus")
                                        .font(.system(size: 13, weight: .semibold))
                                }
                                .buttonStyle(.plain)
                            }
                            .contentShape(Rectangle())
                            .onTapGesture { onAirspaceTap(airspace) }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 8)
                            .background(Color.black.opacity(0.06), in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                        }
                    }
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}
