package cz.miroslavpasek.pigeonnavigator.search

class SearchUseCase(private val repository: SearchRepository) {
    fun execute(query: String): List<String> =
        if (query.isBlank()) emptyList()
        else repository.search(query)
}