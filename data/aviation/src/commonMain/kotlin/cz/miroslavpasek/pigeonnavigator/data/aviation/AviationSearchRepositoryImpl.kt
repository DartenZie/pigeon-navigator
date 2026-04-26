package cz.miroslavpasek.pigeonnavigator.data.aviation

import cz.miroslavpasek.pigeonnavigator.core.util.result.AppResult
import cz.miroslavpasek.pigeonnavigator.core.util.result.appResultOf
import cz.miroslavpasek.pigeonnavigator.data.aviation.db.AviationDatabase
import cz.miroslavpasek.pigeonnavigator.domain.failure.Failure
import cz.miroslavpasek.pigeonnavigator.domain.search.SearchRepository

/**
 * Searches aviation reference data from the active installed package.
 */
class AviationSearchRepositoryImpl(
    private val database: AviationDatabase,
    private val limit: Long = SEARCH_RESULT_LIMIT
) : SearchRepository {

    override suspend fun search(query: String): AppResult<List<String>, Failure> =
        appResultOf(
            block = {
                val normalizedQuery = query.trim()
                if (normalizedQuery.isEmpty()) {
                    return@appResultOf emptyList()
                }

                val queries = database.aviationDatabaseQueries
                val activePackage = queries.selectActivePackage().executeAsOneOrNull()
                    ?: return@appResultOf emptyList()

                val airports = queries.searchAirports(
                    package_id = activePackage.package_id,
                    value_ = normalizedQuery,
                    value__ = normalizedQuery,
                    value___ = limit
                ).executeAsList().map { row ->
                    "${row.airport_id} · ${row.name} · Airport"
                }

                val navaids = queries.searchNavaids(
                    package_id = activePackage.package_id,
                    value_ = normalizedQuery,
                    value__ = normalizedQuery,
                    value___ = limit
                ).executeAsList().map { row ->
                    "${row.navaid_id} · ${row.name} · ${row.kind}"
                }

                val airspaces = queries.searchAirspaces(
                    package_id = activePackage.package_id,
                    value_ = normalizedQuery,
                    value__ = normalizedQuery,
                    value___ = limit
                ).executeAsList().map { row ->
                    "${row.airspace_id} · ${row.name} · ${row.kind}"
                }

                (airports + navaids + airspaces).take(limit.toInt())
            },
            mapError = {
                Failure.Unexpected
            }
        )

    private companion object {
        const val SEARCH_RESULT_LIMIT = 20L
    }
}
