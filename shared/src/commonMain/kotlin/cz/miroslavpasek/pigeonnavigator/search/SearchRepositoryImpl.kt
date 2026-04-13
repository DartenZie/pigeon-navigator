package cz.miroslavpasek.pigeonnavigator.search

/**
 * Provides in-memory search data for the legacy shared search flow.
 */
class SearchRepositoryImpl : SearchRepository {
    private val data = listOf("Apple", "Banana", "Cherry", "Date", "Elderberry")

    /**
     * Returns labels containing [query] using case-insensitive matching.
     */
    override fun search(query: String): List<String> =
        data.filter { it.contains(query, ignoreCase = true) }
}
