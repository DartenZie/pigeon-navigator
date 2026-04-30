//
//  KoinHelper.swift
//  iosApp
//
//  Created by Miroslav Pašek on 21.02.2026.
//

import Shared

/// Resolves shared search store handles for Swift-side consumers.
class SearchStoreHelper {
    /// Creates a new `SearchStoreHandle` from the shared dependency graph.
    static func resolve() -> SearchStoreHandle {
        return KoinHelper().getSearchStoreHandle()
    }
}

/// Resolves shared map tap lookup handles for Swift-side consumers.
class MapTapLookupHelper {
    /// Creates a new `MapTapLookupHandle` from the shared dependency graph.
    static func resolve() -> MapTapLookupHandle {
        return KoinHelper().getMapTapLookupHandle()
    }
}

/// Resolves shared terrain warning handles for Swift-side consumers.
class TerrainWarningHelper {
    /// Creates a new `TerrainWarningHandle` from the shared dependency graph.
    static func resolve() -> TerrainWarningHandle {
        return KoinHelper().getTerrainWarningHandle()
    }
}

/// Resolves shared projected airspace warning handles for Swift-side consumers.
class AirspaceWarningHelper {
    /// Creates a new `AirspaceWarningHandle` from the shared dependency graph.
    static func resolve() -> AirspaceWarningHandle {
        return KoinHelper().getAirspaceWarningHandle()
    }
}

/// Resolves shared search dock handles for Swift-side consumers.
class SearchDockHelper {
    /// Creates a new `SearchDockHandle` from the shared dependency graph.
    static func resolve() -> SearchDockHandle {
        return KoinHelper().getSearchDockHandle()
    }
}

/// Resolves shared settings handles for Swift-side consumers.
class SettingsHelper {
    /// Creates a new `SettingsHandle` from the shared dependency graph.
    static func resolve() -> SettingsHandle {
        return KoinHelper().getSettingsHandle()
    }
}
