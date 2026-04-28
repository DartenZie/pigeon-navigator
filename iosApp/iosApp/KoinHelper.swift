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

/// Resolves shared search dock handles for Swift-side consumers.
class SearchDockHelper {
    /// Creates a new `SearchDockHandle` from the shared dependency graph.
    static func resolve() -> SearchDockHandle {
        return KoinHelper().getSearchDockHandle()
    }
}
