package cz.miroslavpasek.pigeonnavigator.data.search.local

/**
 * Reads searchable records from a local source.
 */
interface LocalSearchDataSource {
    /**
     * Returns records matching [query].
     *
     * @param query User-provided query text.
     */
    suspend fun search(query: String): List<LocalSearchRecord>
}
