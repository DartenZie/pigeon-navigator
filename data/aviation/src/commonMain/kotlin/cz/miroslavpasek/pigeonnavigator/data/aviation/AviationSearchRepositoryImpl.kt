package cz.miroslavpasek.pigeonnavigator.data.aviation

import cz.miroslavpasek.pigeonnavigator.core.util.result.AppResult
import cz.miroslavpasek.pigeonnavigator.core.util.result.appResultOf
import cz.miroslavpasek.pigeonnavigator.data.aviation.db.AviationDatabase
import cz.miroslavpasek.pigeonnavigator.domain.failure.Failure
import cz.miroslavpasek.pigeonnavigator.domain.search.GeoBounds
import cz.miroslavpasek.pigeonnavigator.domain.search.SearchRepository
import cz.miroslavpasek.pigeonnavigator.domain.search.SearchResult

/**
 * Searches aviation reference data from the active installed package.
 */
class AviationSearchRepositoryImpl(
    private val database: AviationDatabase,
    private val limit: Long = SEARCH_RESULT_LIMIT
) : SearchRepository {

    override suspend fun search(query: String): AppResult<List<SearchResult>, Failure> =
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
                    SearchResult.Airport(
                        id = "airport:${row.airport_id}",
                        title = row.airport_id,
                        subtitle = row.name,
                        latitude = row.lat_deg,
                        longitude = row.lon_deg
                    )
                }

                val navaids = queries.searchNavaids(
                    package_id = activePackage.package_id,
                    value_ = normalizedQuery,
                    value__ = normalizedQuery,
                    value___ = limit
                ).executeAsList().map { row ->
                    SearchResult.Navaid(
                        id = "navaid:${row.navaid_key}",
                        title = row.navaid_id,
                        subtitle = "${row.name} · ${row.kind}",
                        frequency = row.frequency,
                        latitude = row.lat_deg,
                        longitude = row.lon_deg
                    )
                }

                val airspaces = queries.searchAirspaces(
                    package_id = activePackage.package_id,
                    value_ = normalizedQuery,
                    value__ = normalizedQuery,
                    value___ = limit
                ).executeAsList().map { row ->
                    val bounds = GeoBounds(
                        minLatitude = row.bbox_min_lat,
                        minLongitude = row.bbox_min_lon,
                        maxLatitude = row.bbox_max_lat,
                        maxLongitude = row.bbox_max_lon
                    )
                    SearchResult.Airspace(
                        id = "airspace:${row.airspace_id}",
                        title = row.name.ifBlank { row.airspace_id },
                        subtitle = row.kind,
                        centerLatitude = bounds.center.latitude,
                        centerLongitude = bounds.center.longitude,
                        bounds = bounds
                    )
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
