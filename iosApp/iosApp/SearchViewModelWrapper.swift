//
//  SearchViewModelWrapper.swift
//  iosApp
//
//  Created by Miroslav Pašek on 21.02.2026.
//

import SwiftUI
import Combine
import Shared

@MainActor
class SearchViewModelWrapper: ObservableObject {
    private let helper = KoinHelper()
    private let store: SearchStoreHandle

    @Published var query: String = ""
    @Published var results: [String] = []
    @Published var isLoading: Bool = false
    @Published var errorMessage: String? = nil

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

    func onQueryChange(_ newQuery: String) {
        store.onQueryChanged(query: newQuery)
    }

    func submitSearch() {
        store.submitSearch()
    }

    func clearSearch() {
        store.clearSearch()
    }

    deinit {
        store.close()
    }
}
