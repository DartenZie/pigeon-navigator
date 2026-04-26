package cz.miroslavpasek.pigeonnavigator.data.search

import cz.miroslavpasek.pigeonnavigator.core.util.result.AppResult
import cz.miroslavpasek.pigeonnavigator.core.util.result.appResultOf
import cz.miroslavpasek.pigeonnavigator.data.search.local.InMemoryLocalSearchDataSource
import cz.miroslavpasek.pigeonnavigator.data.search.local.LocalSearchDataSource
import cz.miroslavpasek.pigeonnavigator.domain.failure.Failure
import cz.miroslavpasek.pigeonnavigator.domain.search.SearchRepository
import cz.miroslavpasek.pigeonnavigator.domain.search.SearchResult

/**
 * Implements [SearchRepository] using a local search data source.
 */
class SearchRepositoryImpl(
    private val localDataSource: LocalSearchDataSource = InMemoryLocalSearchDataSource()
) : SearchRepository {
    /**
     * Returns labels matching [query] by reading local records and mapping them to domain strings.
     *
     * Any thrown exception is mapped to [Failure.Unexpected].
     */
    override suspend fun search(query: String): AppResult<List<SearchResult>, Failure> =
        appResultOf(
            block = {
                localDataSource.search(query).mapIndexed { index, record ->
                    SearchResult.Airport(
                        id = "local:$index:${record.label}",
                        title = record.label,
                        subtitle = "Local result",
                        latitude = 0.0,
                        longitude = 0.0
                    )
                }
            },
            mapError = {
                Failure.Unexpected
            }
        )
}
