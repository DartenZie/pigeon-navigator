package cz.miroslavpasek.pigeonnavigator.search

/**
 * Executes local search while guarding against blank input.
 */
class SearchUseCase(private val repository: SearchRepository) {
    /**
     * Returns an empty list for blank queries, otherwise delegates to [SearchRepository].
     */
    fun execute(query: String): List<String> =
        if (query.isBlank()) emptyList()
        else repository.search(query)
}
