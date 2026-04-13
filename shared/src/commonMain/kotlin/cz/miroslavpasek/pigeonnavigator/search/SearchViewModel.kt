package cz.miroslavpasek.pigeonnavigator.search

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds query and result state for the legacy shared search UI.
 */
class SearchViewModel(private val searchUseCase: SearchUseCase) {

    private val _query = MutableStateFlow("")
    private val _results = MutableStateFlow<List<String>>(emptyList())

    val query: StateFlow<String> = _query.asStateFlow()
    val results: StateFlow<List<String>> = _results.asStateFlow()

    /**
     * Updates query and recomputes results synchronously.
     */
    fun onQueryChanged(newQuery: String) {
        _query.value = newQuery
        _results.value = searchUseCase.execute(newQuery)
    }

    /**
     * Resets query and search results.
     */
    fun clearSearch() {
        _query.value = ""
        _results.value = emptyList()
    }
}
