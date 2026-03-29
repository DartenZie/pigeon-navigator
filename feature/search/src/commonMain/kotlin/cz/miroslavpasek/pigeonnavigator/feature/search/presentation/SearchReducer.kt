package cz.miroslavpasek.pigeonnavigator.feature.search.presentation

/**
 * Pure reducer for search state transitions.
 */
class SearchReducer {
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
