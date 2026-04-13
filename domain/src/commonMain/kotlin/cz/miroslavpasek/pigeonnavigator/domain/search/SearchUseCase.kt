package cz.miroslavpasek.pigeonnavigator.domain.search

import cz.miroslavpasek.pigeonnavigator.core.util.result.AppResult
import cz.miroslavpasek.pigeonnavigator.domain.failure.Failure

/**
 * Validates user search input and delegates matching to the repository.
 */
class SearchUseCase(
    private val repository: SearchRepository
) {
    /**
     * Executes a search for [query].
     *
     * @param query User-provided text to search.
     * @return [AppResult.Failure] with [Failure.Validation] when [query] is blank.
     * Otherwise returns repository output unchanged.
     */
    suspend operator fun invoke(query: String): AppResult<List<String>, Failure> {
        if (query.isBlank()) {
            return AppResult.Failure(Failure.Validation("Query cannot be blank"))
        }

        return repository.search(query)
    }
}
