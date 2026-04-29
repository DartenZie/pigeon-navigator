import SwiftUI
import Combine
import Shared

/// Bridges the shared Kotlin `SettingsHandle` into an observable SwiftUI model so
/// platform views can react to global app settings (units, warning threshold,
/// search debounce / minimum query length, map zoom and bearing tuning) without
/// reaching into Koin directly.
@MainActor
final class AppSettingsViewModelWrapper: ObservableObject {
    private let handle: SettingsHandle
    private var isDisposed = false

    // Unit preferences
    @Published var distanceUnit: DomainDistanceUnit = DomainDistanceUnit.nauticalmiles
    @Published var altitudeUnit: DomainAltitudeUnit = DomainAltitudeUnit.feet
    @Published var speedUnit: DomainSpeedUnit = DomainSpeedUnit.knots

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

    func updateDistanceUnit(_ unit: DomainDistanceUnit) {
        handle.updateUnits(
            units: DomainUnitPreferences(distance: unit, altitude: altitudeUnit, speed: speedUnit)
        ) { _ in }
    }

    func updateAltitudeUnit(_ unit: DomainAltitudeUnit) {
        handle.updateUnits(
            units: DomainUnitPreferences(distance: distanceUnit, altitude: unit, speed: speedUnit)
        ) { _ in }
    }

    func updateSpeedUnit(_ unit: DomainSpeedUnit) {
        handle.updateUnits(
            units: DomainUnitPreferences(distance: distanceUnit, altitude: altitudeUnit, speed: unit)
        ) { _ in }
    }

    func updateUnits(distance: DomainDistanceUnit, altitude: DomainAltitudeUnit, speed: DomainSpeedUnit) {
        handle.updateUnits(
            units: DomainUnitPreferences(distance: distance, altitude: altitude, speed: speed)
        ) { _ in }
    }

    func updateTimeToCollisionWarningSeconds(_ seconds: Int32) {
        handle.updateTimeToCollisionWarningSeconds(seconds: seconds) { _ in }
    }

    func updateSearchDebounceMillis(_ milliseconds: Int64) {
        handle.updateSearchPreferences(
            searchDebounceMillis: milliseconds,
            minimumQueryLength: minimumQueryLength
        ) { _ in }
    }

    func updateMinimumQueryLength(_ length: Int32) {
        handle.updateSearchPreferences(
            searchDebounceMillis: searchDebounceMillis,
            minimumQueryLength: length
        ) { _ in }
    }

    func updateMaxDynamicZoomSpeedKmh(_ speed: Double) {
        handle.updateMapPreferences(
            maxDynamicZoomSpeedKmh: speed,
            maxSpeedZoomOutDelta: maxSpeedZoomOutDelta,
            bearingUpdateThresholdDegrees: bearingUpdateThresholdDegrees
        ) { _ in }
    }

    func updateMaxSpeedZoomOutDelta(_ delta: Double) {
        handle.updateMapPreferences(
            maxDynamicZoomSpeedKmh: maxDynamicZoomSpeedKmh,
            maxSpeedZoomOutDelta: delta,
            bearingUpdateThresholdDegrees: bearingUpdateThresholdDegrees
        ) { _ in }
    }

    func updateBearingUpdateThresholdDegrees(_ degrees: Double) {
        handle.updateMapPreferences(
            maxDynamicZoomSpeedKmh: maxDynamicZoomSpeedKmh,
            maxSpeedZoomOutDelta: maxSpeedZoomOutDelta,
            bearingUpdateThresholdDegrees: degrees
        ) { _ in }
    }

    func dispose() {
        guard !isDisposed else { return }
        isDisposed = true
        handle.close()
    }

    deinit {
        if !isDisposed {
            handle.close()
        }
    }
}
