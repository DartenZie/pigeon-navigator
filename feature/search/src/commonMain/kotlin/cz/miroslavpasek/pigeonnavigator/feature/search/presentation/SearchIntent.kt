package cz.miroslavpasek.pigeonnavigator.feature.search.presentation

/**
 * User and system inputs for the search feature state machine.
 */
sealed interface SearchIntent {
    data class QueryChanged(val query: String) : SearchIntent
    data object SubmitSearch : SearchIntent
    data object ClearSearch : SearchIntent
    data class SearchSucceeded(val results: List<String>) : SearchIntent
    data class SearchFailed(val message: String) : SearchIntent
}
