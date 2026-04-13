package cz.miroslavpasek.pigeonnavigator.search

/**
 * Defines search operations for the legacy shared search flow.
 */
interface SearchRepository {
    /**
     * Returns labels matching [query].
     */
    fun search(query: String): List<String>
}
