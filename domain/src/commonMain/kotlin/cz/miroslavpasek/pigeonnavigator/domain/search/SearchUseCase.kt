package cz.miroslavpasek.pigeonnavigator.domain.search

import cz.miroslavpasek.pigeonnavigator.core.util.result.AppResult
import cz.miroslavpasek.pigeonnavigator.domain.failure.Failure

/**
 * Validates search input and delegates to [SearchRepository].
 */
class SearchUseCase(
    private val repository: SearchRepository
) {
    /**
     * Executes search for [query].
     */
    suspend operator fun invoke(query: String): AppResult<List<String>, Failure> {
        if (query.isBlank()) {
            return AppResult.Failure(Failure.Validation("Query cannot be blank"))
        }

        return repository.search(query)
    }
}
