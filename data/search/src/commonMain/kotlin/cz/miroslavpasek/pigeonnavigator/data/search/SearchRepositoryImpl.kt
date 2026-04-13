package cz.miroslavpasek.pigeonnavigator.data.search

import cz.miroslavpasek.pigeonnavigator.core.util.result.AppResult
import cz.miroslavpasek.pigeonnavigator.core.util.result.appResultOf
import cz.miroslavpasek.pigeonnavigator.data.search.local.InMemoryLocalSearchDataSource
import cz.miroslavpasek.pigeonnavigator.data.search.local.LocalSearchDataSource
import cz.miroslavpasek.pigeonnavigator.data.search.mapper.toDomainSearchItems
import cz.miroslavpasek.pigeonnavigator.domain.failure.Failure
import cz.miroslavpasek.pigeonnavigator.domain.search.SearchRepository

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
    override suspend fun search(query: String): AppResult<List<String>, Failure> =
        appResultOf(
            block = {
                localDataSource.search(query).toDomainSearchItems()
            },
            mapError = {
                Failure.Unexpected
            }
        )
}
