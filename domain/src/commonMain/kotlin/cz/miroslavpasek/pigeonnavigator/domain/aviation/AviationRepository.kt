package cz.miroslavpasek.pigeonnavigator.domain.aviation

import cz.miroslavpasek.pigeonnavigator.core.util.result.AppResult
import cz.miroslavpasek.pigeonnavigator.domain.failure.Failure

/**
 * Domain-facing contract for installing packages and querying aviation data by coordinate.
 */
interface AviationRepository {
    /**
     * Installs and activates a package from [source].
     */
    suspend fun installPackage(source: AviationPackageSource): AppResult<Unit, Failure>

    /**
     * Returns active package PMTiles paths used by map rendering and terrain sampling.
     */
    suspend fun getActiveMapPackage(): AppResult<ActiveMapPackage, Failure>

    /**
     * Returns true when an active package record exists in the database **and** its
     * PMTiles files are present on disk.  Use this to detect the case where a DB record
     * survived a reinstall but the associated files were wiped.
     */
    suspend fun activePackageFilesExist(): Boolean

    /**
     * Returns airports near a coordinate sorted by ascending distance.
     */
    suspend fun nearbyAirports(
        latitude: Double,
        longitude: Double,
        radiusMeters: Double = DEFAULT_RADIUS_METERS,
        limit: Int = DEFAULT_LIMIT
    ): AppResult<List<NearbyAirport>, Failure>

    /**
     * Returns navaids near a coordinate sorted by ascending distance.
     */
    suspend fun nearbyNavaids(
        latitude: Double,
        longitude: Double,
        radiusMeters: Double = DEFAULT_RADIUS_METERS,
        limit: Int = DEFAULT_LIMIT
    ): AppResult<List<NearbyNavaid>, Failure>

    /**
     * Returns airspaces containing a coordinate.
     */
    suspend fun containingAirspaces(
        latitude: Double,
        longitude: Double
    ): AppResult<List<Airspace>, Failure>

    companion object {
        const val DEFAULT_LIMIT = 50
        const val DEFAULT_RADIUS_METERS = 50_000.0
    }
}
