//
//  KoinHelper.swift
//  iosApp
//
//  Created by Miroslav Pašek on 21.02.2026.
//

import Shared

class SearchStoreHelper {
    static func resolve() -> SearchStoreHandle {
        return KoinHelper().getSearchStoreHandle()
    }
}
