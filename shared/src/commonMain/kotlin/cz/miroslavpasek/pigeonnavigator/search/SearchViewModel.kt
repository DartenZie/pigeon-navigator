package cz.miroslavpasek.pigeonnavigator.search

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SearchViewModel(private val searchUseCase: SearchUseCase) {

    private val _query = MutableStateFlow("")
    private val _results = MutableStateFlow<List<String>>(emptyList())

    val query: StateFlow<String> = _query.asStateFlow()
    val results: StateFlow<List<String>> = _results.asStateFlow()

    fun onQueryChanged(newQuery: String) {
        _query.value = newQuery
        _results.value = searchUseCase.execute(newQuery)
    }

    fun clearSearch() {
        _query.value = ""
        _results.value = emptyList()
    }
}