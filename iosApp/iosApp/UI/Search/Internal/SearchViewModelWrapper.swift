//
//  SearchViewModelWrapper.swift
//  iosApp
//
//  Created by Miroslav Pašek on 21.02.2026.
//

import SwiftUI
import Combine
import Shared

/// Bridges the shared Kotlin `SearchStoreHandle` into an observable SwiftUI model.
@MainActor
class SearchViewModelWrapper: ObservableObject {
    private let helper = KoinHelper()
    private let store: SearchStoreHandle

    @Published var query: String = ""
    @Published var results: [String] = []
    @Published var isLoading: Bool = false
    @Published var errorMessage: String? = nil

    /// Creates the wrapper and starts observing shared store state updates.
    init() {
        self.store = helper.getSearchStoreHandle()
        observeState()
    }

    private func observeState() {
        store.startState { [weak self] state in
            guard let self else { return }
            self.query = state.query
            self.results = state.results
            self.isLoading = state.isLoading
            self.errorMessage = state.errorMessage
        }
    }

    /// Sends updated query text to the shared search store.
    func onQueryChange(_ newQuery: String) {
        store.onQueryChanged(query: newQuery)
    }

    /// Requests search execution for the current query.
    func submitSearch() {
        store.submitSearch()
    }

    /// Clears query and results in the shared search store.
    func clearSearch() {
        store.clearSearch()
    }

    deinit {
        store.close()
    }
}
