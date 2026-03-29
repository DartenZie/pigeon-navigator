package cz.miroslavpasek.pigeonnavigator.search

class SearchRepositoryImpl : SearchRepository {
    private val data = listOf("Apple", "Banana", "Cherry", "Date", "Elderberry")

    override fun search(query: String): List<String> =
        data.filter { it.contains(query, ignoreCase = true) }
}