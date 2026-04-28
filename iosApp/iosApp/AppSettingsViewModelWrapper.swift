import SwiftUI
import Combine
import Shared

/// Bridges the shared Kotlin `SettingsHandle` into an observable SwiftUI model so
/// platform views can read and mutate global app settings (units, warning threshold,
/// search debounce / minimum query length, map zoom & bearing tuning) without
/// reaching into Koin directly.
@MainActor
final class AppSettingsViewModelWrapper: ObservableObject {
    private let handle: SettingsHandle

    // Unit preferences mirror Kotlin enums directly.
    @Published var distanceUnit: DomainDistanceUnit = .nauticalmiles
    @Published var altitudeUnit: DomainAltitudeUnit = .feet
    @Published var speedUnit: DomainSpeedUnit = .knots

    // Warning preferences
    @Published var timeToCollisionWarningSeconds: Int32 = 60

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
            self.distanceUnit = settings.units.distance
            self.altitudeUnit = settings.units.altitude
            self.speedUnit = settings.units.speed
            self.timeToCollisionWarningSeconds = settings.warning.timeToCollisionWarningSeconds
            self.searchDebounceMillis = settings.search.searchDebounceMillis
            self.minimumQueryLength = settings.search.minimumQueryLength
            self.maxDynamicZoomSpeedKmh = settings.map.maxDynamicZoomSpeedKmh
            self.maxSpeedZoomOutDelta = settings.map.maxSpeedZoomOutDelta
            self.bearingUpdateThresholdDegrees = settings.map.bearingUpdateThresholdDegrees
        }
    }

    /// Cancels active subscriptions and the underlying Kotlin coroutine scope.
    /// Call this from the SwiftUI scene/lifecycle teardown to avoid relying on
    /// process lifetime alone.
    func dispose() {
        handle.close()
    }

    deinit {
        handle.close()
    }

    // MARK: - Mutations

    func updateUnits(distance: DomainDistanceUnit, altitude: DomainAltitudeUnit, speed: DomainSpeedUnit) {
        handle.updateUnits(
            units: DomainUnitPreferences(distance: distance, altitude: altitude, speed: speed)
        ) { _ in }
    }

    func updateTimeToCollisionWarningSeconds(_ seconds: Int32) {
        handle.updateTimeToCollisionWarningSeconds(seconds: seconds) { _ in }
    }

    func updateSearchPreferences(searchDebounceMillis: Int64, minimumQueryLength: Int32) {
        handle.updateSearchPreferences(
            searchDebounceMillis: searchDebounceMillis,
            minimumQueryLength: minimumQueryLength
        ) { _ in }
    }

    func updateMapPreferences(
        maxDynamicZoomSpeedKmh: Double,
        maxSpeedZoomOutDelta: Double,
        bearingUpdateThresholdDegrees: Double
    ) {
        handle.updateMapPreferences(
            maxDynamicZoomSpeedKmh: maxDynamicZoomSpeedKmh,
            maxSpeedZoomOutDelta: maxSpeedZoomOutDelta,
            bearingUpdateThresholdDegrees: bearingUpdateThresholdDegrees
        ) { _ in }
    }
}
