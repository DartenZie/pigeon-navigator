package cz.miroslavpasek.pigeonnavigator.feature.search.presentation

/**
 * Defines user and internal inputs processed by the search reducer and store.
 */
sealed interface SearchIntent {
    /** Updates query text before any search execution. */
    data class QueryChanged(val query: String) : SearchIntent

    /** Requests search execution for the current query state. */
    data object SubmitSearch : SearchIntent

    /** Resets query, results, and transient errors. */
    data object ClearSearch : SearchIntent

    /** Applies successful search results to state. */
    data class SearchSucceeded(val results: List<String>) : SearchIntent

    /** Applies a failed search outcome to state. */
    data class SearchFailed(val message: String) : SearchIntent
}
