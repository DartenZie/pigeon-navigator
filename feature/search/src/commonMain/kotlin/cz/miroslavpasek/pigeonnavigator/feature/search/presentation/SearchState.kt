package cz.miroslavpasek.pigeonnavigator.feature.search.presentation

/**
 * Full immutable UI state for the search feature.
 */
data class SearchState(
    val query: String = "",
    val results: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
