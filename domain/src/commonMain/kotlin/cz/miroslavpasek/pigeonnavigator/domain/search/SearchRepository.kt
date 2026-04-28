package cz.miroslavpasek.pigeonnavigator.domain.search

import cz.miroslavpasek.pigeonnavigator.core.util.result.AppResult
import cz.miroslavpasek.pigeonnavigator.domain.failure.Failure

/**
 * Defines search operations exposed to domain use cases.
 */
interface SearchRepository {
    /**
     * Returns aviation records that match [query].
     *
     * @param query User-provided search text.
     * @return [AppResult.Success] with matched records, or [AppResult.Failure] with a domain failure.
     */
    suspend fun search(query: String): AppResult<List<SearchResult>, Failure>
}
