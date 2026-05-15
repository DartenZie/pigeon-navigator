import SwiftUI
import Shared

/// Picker rows for selecting the active aircraft-location source and UDP format.
struct LocationSection: View {
    let source: DomainLocationSource
    let udpFormat: DomainUdpLocationFormat
    let onLocationChange: (
        _ source: DomainLocationSource,
        _ udpFormat: DomainUdpLocationFormat
    ) -> Void

    var body: some View {
        Section(header: Text("Location")) {
            Picker("Source", selection: Binding(
                get: { source },
                set: { newValue in onLocationChange(newValue, udpFormat) }
            )) {
                ForEach(LocationSection.sourceOptions, id: \.0) { (source, label) in
                    Text(label).tag(source)
                }
            }

            if source == DomainLocationSource.udp {
                Picker("UDP format", selection: Binding(
                    get: { udpFormat },
                    set: { newValue in onLocationChange(source, newValue) }
                )) {
                    ForEach(LocationSection.formatOptions, id: \.0) { (format, label) in
                        Text(label).tag(format)
                    }
                }
            }
        }
    }

    private static let sourceOptions: [(DomainLocationSource, String)] = [
        (.devicegps, "GPS"),
        (.udp, "UDP"),
    ]

    private static let formatOptions: [(DomainUdpLocationFormat, String)] = [
        (.msfs, "MSFS"),
        (.xplane, "X-Plane"),
    ]
}
