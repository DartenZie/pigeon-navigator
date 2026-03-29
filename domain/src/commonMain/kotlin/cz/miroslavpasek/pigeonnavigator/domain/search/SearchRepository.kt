package cz.miroslavpasek.pigeonnavigator.domain.search

import cz.miroslavpasek.pigeonnavigator.core.util.result.AppResult
import cz.miroslavpasek.pigeonnavigator.domain.failure.Failure

/**
 * Domain contract for searching queryable items.
 */
interface SearchRepository {
    /**
     * Searches for items matching [query].
     */
    suspend fun search(query: String): AppResult<List<String>, Failure>
}
