package cz.miroslavpasek.pigeonnavigator.data.search.local

/**
 * Local in-memory implementation used as the initial search data source.
 */
class InMemoryLocalSearchDataSource : LocalSearchDataSource {
    private val records = listOf(
        LocalSearchRecord("Apple"),
        LocalSearchRecord("Banana"),
        LocalSearchRecord("Cherry"),
        LocalSearchRecord("Date"),
        LocalSearchRecord("Elderberry")
    )

    override suspend fun search(query: String): List<LocalSearchRecord> =
        records.filter { it.label.contains(query, ignoreCase = true) }
}
