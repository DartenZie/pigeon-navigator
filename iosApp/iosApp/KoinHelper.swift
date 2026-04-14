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
