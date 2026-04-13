package cz.miroslavpasek.pigeonnavigator.feature.search.presentation

/**
 * Applies pure state transitions for the search feature.
 */
class SearchReducer {
    /**
     * Returns the next [SearchState] for the supplied [intent].
     *
     * This function is side-effect free.
     */
    fun reduce(state: SearchState, intent: SearchIntent): SearchState =
        when (intent) {
            is SearchIntent.QueryChanged -> {
                state.copy(
                    query = intent.query,
                    errorMessage = null
                )
            }

            SearchIntent.SubmitSearch -> {
                state.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }

            SearchIntent.ClearSearch -> SearchState()

            is SearchIntent.SearchSucceeded -> {
                state.copy(
                    results = intent.results,
                    isLoading = false,
                    errorMessage = null
                )
            }

            is SearchIntent.SearchFailed -> {
                state.copy(
                    isLoading = false,
                    errorMessage = intent.message
                )
            }
        }
}
