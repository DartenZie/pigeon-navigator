package cz.miroslavpasek.pigeonnavigator.data.search.local

/**
 * Reads search records from a local source.
 */
interface LocalSearchDataSource {
    /**
     * Returns records matching [query].
     */
    suspend fun search(query: String): List<LocalSearchRecord>
}
