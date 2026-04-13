package cz.miroslavpasek.pigeonnavigator.data.search.local

/**
 * Stores a fixed in-memory record list for local search.
 */
class InMemoryLocalSearchDataSource : LocalSearchDataSource {
    private val records = listOf(
        LocalSearchRecord("Apple"),
        LocalSearchRecord("Banana"),
        LocalSearchRecord("Cherry"),
        LocalSearchRecord("Date"),
        LocalSearchRecord("Elderberry")
    )

    /**
     * Returns records whose labels contain [query] using case-insensitive matching.
     */
    override suspend fun search(query: String): List<LocalSearchRecord> =
        records.filter { it.label.contains(query, ignoreCase = true) }
}
