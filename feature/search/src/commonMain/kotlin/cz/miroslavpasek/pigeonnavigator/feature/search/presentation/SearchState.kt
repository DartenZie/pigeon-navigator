package cz.miroslavpasek.pigeonnavigator.feature.search.presentation

/**
 * Represents the full immutable UI state of the search feature.
 *
 * @property query Current query text.
 * @property results Labels returned by the latest successful search.
 * @property isLoading True while a search request is in progress.
 * @property errorMessage Last user-visible error message, or `null` when there is no error.
 */
data class SearchState(
    val query: String = "",
    val results: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
