import SwiftUI
import Combine
import Shared

/// Bridges the shared Kotlin `SettingsHandle` into an observable SwiftUI model so
/// platform views can react to global app settings (units, warning threshold,
/// search debounce / minimum query length, map zoom & bearing tuning) without
/// reaching into Koin directly.
@MainActor
final class AppSettingsViewModelWrapper: ObservableObject {
    private let handle: SettingsHandle

    // Search preferences
    @Published var searchDebounceMillis: Int64 = 300
    @Published var minimumQueryLength: Int32 = 2

    // Map preferences
    @Published var maxDynamicZoomSpeedKmh: Double = 300.0
    @Published var maxSpeedZoomOutDelta: Double = 2.5
    @Published var bearingUpdateThresholdDegrees: Double = 4.0

    init() {
        self.handle = SettingsHelper.resolve()
        handle.startSettings { [weak self] settings in
            guard let self else { return }
            self.searchDebounceMillis = settings.search.searchDebounceMillis
            self.minimumQueryLength = settings.search.minimumQueryLength
            self.maxDynamicZoomSpeedKmh = settings.map.maxDynamicZoomSpeedKmh
            self.maxSpeedZoomOutDelta = settings.map.maxSpeedZoomOutDelta
            self.bearingUpdateThresholdDegrees = settings.map.bearingUpdateThresholdDegrees
        }
    }

    deinit {
        handle.close()
    }
}
