package cz.miroslavpasek.pigeonnavigator.data.aviation

import cz.miroslavpasek.pigeonnavigator.core.util.result.AppResult
import cz.miroslavpasek.pigeonnavigator.data.aviation.db.AviationDatabase
import cz.miroslavpasek.pigeonnavigator.data.aviation.internal.GeoMath
import cz.miroslavpasek.pigeonnavigator.data.aviation.internal.NavSnapshotParser
import cz.miroslavpasek.pigeonnavigator.data.aviation.internal.OfpkgArchive
import cz.miroslavpasek.pigeonnavigator.data.aviation.internal.ParsedNavSnapshot
import cz.miroslavpasek.pigeonnavigator.data.aviation.platform.OfpkgInstallLogger
import cz.miroslavpasek.pigeonnavigator.data.aviation.platform.PackageAssetResolver
import cz.miroslavpasek.pigeonnavigator.data.aviation.platform.PlatformFileSystem
import cz.miroslavpasek.pigeonnavigator.data.aviation.platform.Sha256Hasher
import cz.miroslavpasek.pigeonnavigator.domain.aviation.ActiveMapPackage
import cz.miroslavpasek.pigeonnavigator.domain.aviation.Airport
import cz.miroslavpasek.pigeonnavigator.domain.aviation.Airspace
import cz.miroslavpasek.pigeonnavigator.domain.aviation.AviationPackageSource
import cz.miroslavpasek.pigeonnavigator.domain.aviation.AviationRepository
import cz.miroslavpasek.pigeonnavigator.domain.aviation.GeoPoint
import cz.miroslavpasek.pigeonnavigator.domain.aviation.Navaid
import cz.miroslavpasek.pigeonnavigator.domain.aviation.NearbyAirport
import cz.miroslavpasek.pigeonnavigator.domain.aviation.NearbyNavaid
import cz.miroslavpasek.pigeonnavigator.domain.failure.Failure
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * SQLDelight-backed repository for OFPKG installation and lat/lon aviation queries.
 */
class AviationRepositoryImpl(
    private val database: AviationDatabase,
    private val fileSystem: PlatformFileSystem = PlatformFileSystem(),
    private val assetResolver: PackageAssetResolver = PackageAssetResolver(),
    private val hasher: Sha256Hasher = Sha256Hasher()
) : AviationRepository {

    private val queries = database.aviationDatabaseQueries
    private val parser = NavSnapshotParser()

    override suspend fun installPackage(source: AviationPackageSource): AppResult<Unit, Failure> {
        val installStartedAt = nowEpochMillis()
        val packagePath = resolveSourcePath(source) ?: return AppResult.Failure(Failure.DataUnavailable)
        logInstall("Install started from $packagePath")
        val packageBytes = fileSystem.readBytes(packagePath) ?: return AppResult.Failure(Failure.DataUnavailable)

        val archive = OfpkgArchive(packageBytes)
        val manifestBytes = archive.readEntry(MANIFEST_PATH) ?: return AppResult.Failure(Failure.DataUnavailable)
        val checksumsBytes = archive.readEntry(CHECKSUMS_PATH) ?: return AppResult.Failure(Failure.DataUnavailable)
        val navXmlBytes = archive.readEntry(NAV_XML_PATH) ?: return AppResult.Failure(Failure.DataUnavailable)
        val mapPmtilesBytes = archive.readEntry(MAP_PMTILES_PATH) ?: return AppResult.Failure(Failure.DataUnavailable)
        val terrainPmtilesBytes = archive.readEntry(TERRAIN_PMTILES_PATH) ?: return AppResult.Failure(Failure.DataUnavailable)

        val checksumMap = parseChecksumFile(checksumsBytes.decodeToString())
        if (!verifyChecksum(checksumMap, NAV_XML_PATH, navXmlBytes) ||
            !verifyChecksum(checksumMap, MAP_PMTILES_PATH, mapPmtilesBytes) ||
            !verifyChecksum(checksumMap, TERRAIN_PMTILES_PATH, terrainPmtilesBytes)
        ) {
            return AppResult.Failure(Failure.DataUnavailable)
        }

        val manifest = parseManifest(manifestBytes.decodeToString())
        val parsedNavSnapshot = parser.parse(navXmlBytes.decodeToString())
        val packageId = buildPackageId(manifest)
        logInstall(
            "Parsed package $packageId with ${parsedNavSnapshot.airports.size} airports, " +
                "${parsedNavSnapshot.navaids.size} navaids, ${parsedNavSnapshot.airspaces.size} airspaces"
        )

        val rootPath = pathJoin(fileSystem.filesDirectoryPath, PACKAGE_ROOT_DIR)
        val tempInstallPath = pathJoin(rootPath, "$packageId.tmp")
        val finalInstallPath = pathJoin(rootPath, packageId)

        fileSystem.createDirectories(rootPath)
        fileSystem.deleteRecursively(tempInstallPath)
        fileSystem.createDirectories(tempInstallPath)

        val mapOutputPath = pathJoin(tempInstallPath, MAP_PMTILES_NAME)
        val terrainOutputPath = pathJoin(tempInstallPath, TERRAIN_PMTILES_NAME)
        val xmlOutputPath = pathJoin(tempInstallPath, NAV_XML_NAME)

        val writesSucceeded = fileSystem.writeBytes(mapOutputPath, mapPmtilesBytes) &&
            fileSystem.writeBytes(terrainOutputPath, terrainPmtilesBytes) &&
            fileSystem.writeBytes(xmlOutputPath, navXmlBytes)

        if (!writesSucceeded) {
            fileSystem.deleteRecursively(tempInstallPath)
            logInstall("Install failed during filesystem write phase")
            return AppResult.Failure(Failure.DataUnavailable)
        }

        fileSystem.deleteRecursively(finalInstallPath)
        if (!fileSystem.move(tempInstallPath, finalInstallPath)) {
            fileSystem.deleteRecursively(tempInstallPath)
            logInstall("Install failed during temp->final move phase")
            return AppResult.Failure(Failure.DataUnavailable)
        }

        val finalMapPath = pathJoin(finalInstallPath, MAP_PMTILES_NAME)
        val finalTerrainPath = pathJoin(finalInstallPath, TERRAIN_PMTILES_NAME)

        val previousActivePackageId = activePackageRecord()?.packageId

        val transactionSucceeded = runCatching {
            queries.transaction {
                previousActivePackageId?.let { oldId ->
                    deletePackageRows(oldId)
                    queries.deletePackage(oldId)
                }
                queries.clearActivePackage()
                queries.insertInstalledPackage(
                    package_id = packageId,
                    cycle = manifest.cycle,
                    region = manifest.region,
                    source = manifest.source,
                    installed_at_epoch_ms = nowEpochMillis(),
                    is_active = 1,
                    map_pmtiles_path = finalMapPath,
                    terrain_pmtiles_path = finalTerrainPath,
                    xml_sha256 = hasher.hashHex(navXmlBytes),
                    map_sha256 = hasher.hashHex(mapPmtilesBytes),
                    terrain_sha256 = hasher.hashHex(terrainPmtilesBytes)
                )
                insertAviationData(packageId = packageId, snapshot = parsedNavSnapshot)
            }
        }.isSuccess

        if (!transactionSucceeded) {
            fileSystem.deleteRecursively(finalInstallPath)
            logInstall("Install failed during database transaction phase")
            return AppResult.Failure(Failure.Unexpected)
        }

        previousActivePackageId
            ?.takeIf { it != packageId }
            ?.let { oldId ->
                fileSystem.deleteRecursively(pathJoin(rootPath, oldId))
            }

        val installDurationMs = nowEpochMillis() - installStartedAt
        logInstall("Install finished for $packageId in ${installDurationMs}ms")

        return AppResult.Success(Unit)
    }

    override suspend fun getActiveMapPackage(): AppResult<ActiveMapPackage, Failure> {
        val active = activePackageRecord() ?: return AppResult.Failure(Failure.DataUnavailable)
        return AppResult.Success(
            ActiveMapPackage(
                packageId = active.packageId,
                mapPmtilesAbsolutePath = active.mapPmtilesPath,
                terrainPmtilesAbsolutePath = active.terrainPmtilesPath
            )
        )
    }

    override suspend fun nearbyAirports(
        latitude: Double,
        longitude: Double,
        radiusMeters: Double,
        limit: Int
    ): AppResult<List<NearbyAirport>, Failure> {
        val active = activePackageRecord() ?: return AppResult.Failure(Failure.DataUnavailable)
        val bbox = boundingBox(latitude, longitude, radiusMeters)
        val rows = queries.selectAirportsByBoundingBox(
            package_id = active.packageId,
            lat_deg = bbox.minLatitude,
            lat_deg_ = bbox.maxLatitude,
            lon_deg = bbox.minLongitude,
            lon_deg_ = bbox.maxLongitude
        ).executeAsList()

        val items = rows.mapNotNull { row ->
            val distance = GeoMath.distanceMeters(latitude, longitude, row.lat_deg, row.lon_deg)
            if (distance > radiusMeters) {
                null
            } else {
                NearbyAirport(
                    airport = Airport(
                        id = row.airport_id,
                        name = row.name,
                        kind = row.kind,
                        latitude = row.lat_deg,
                        longitude = row.lon_deg,
                        elevationMeters = row.elev_m?.toInt()
                    ),
                    distanceMeters = distance
                )
            }
        }.sortedBy { it.distanceMeters }
            .take(limit.coerceAtLeast(1))

        return AppResult.Success(items)
    }

    override suspend fun nearbyNavaids(
        latitude: Double,
        longitude: Double,
        radiusMeters: Double,
        limit: Int
    ): AppResult<List<NearbyNavaid>, Failure> {
        val active = activePackageRecord() ?: return AppResult.Failure(Failure.DataUnavailable)
        val bbox = boundingBox(latitude, longitude, radiusMeters)
        val rows = queries.selectNavaidsByBoundingBox(
            package_id = active.packageId,
            lat_deg = bbox.minLatitude,
            lat_deg_ = bbox.maxLatitude,
            lon_deg = bbox.minLongitude,
            lon_deg_ = bbox.maxLongitude
        ).executeAsList()

        val items = rows.mapNotNull { row ->
            val distance = GeoMath.distanceMeters(latitude, longitude, row.lat_deg, row.lon_deg)
            if (distance > radiusMeters) {
                null
            } else {
                NearbyNavaid(
                    navaid = Navaid(
                        id = row.navaid_id,
                        name = row.name,
                        kind = row.kind,
                        detail = row.detail,
                        latitude = row.lat_deg,
                        longitude = row.lon_deg
                    ),
                    distanceMeters = distance
                )
            }
        }.sortedBy { it.distanceMeters }
            .take(limit.coerceAtLeast(1))

        return AppResult.Success(items)
    }

    override suspend fun containingAirspaces(
        latitude: Double,
        longitude: Double
    ): AppResult<List<Airspace>, Failure> {
        val active = activePackageRecord() ?: return AppResult.Failure(Failure.DataUnavailable)
        val candidates = queries.selectAirspacesByBoundingBox(
            package_id = active.packageId,
            bbox_min_lat = latitude,
            bbox_max_lat = latitude,
            bbox_min_lon = longitude,
            bbox_max_lon = longitude
        ).executeAsList()

        val result = candidates.mapNotNull { row ->
            val points = queries.selectAirspacePoints(
                package_id = active.packageId,
                airspace_id = row.airspace_id
            ).executeAsList().map { pointRow ->
                GeoPoint(latitude = pointRow.lat_deg, longitude = pointRow.lon_deg)
            }

            if (points.size < 3 || !GeoMath.polygonContains(GeoPoint(latitude, longitude), points)) {
                null
            } else {
                Airspace(
                    id = row.airspace_id,
                    name = row.name,
                    kind = row.kind,
                    lowerLimitMeters = row.lower_m?.toInt(),
                    lowerLimitReference = row.lower_ref,
                    upperLimitMeters = row.upper_m?.toInt(),
                    upperLimitReference = row.upper_ref,
                    points = points
                )
            }
        }

        return AppResult.Success(result)
    }

    private fun insertAviationData(packageId: String, snapshot: ParsedNavSnapshot) {
        snapshot.airports.forEach { airport ->
            queries.insertAirport(
                package_id = packageId,
                airport_id = airport.id,
                name = airport.name,
                kind = airport.kind,
                lat_deg = airport.latitude,
                lon_deg = airport.longitude,
                elev_m = airport.elevationMeters?.toLong()
            )
        }

        val navaidCounters = mutableMapOf<String, Int>()
        snapshot.navaids.forEach { navaid ->
            val count = (navaidCounters[navaid.id] ?: 0) + 1
            navaidCounters[navaid.id] = count
            queries.insertNavaid(
                package_id = packageId,
                navaid_key = if (count == 1) navaid.id else "${navaid.id}#$count",
                navaid_id = navaid.id,
                name = navaid.name,
                kind = navaid.kind,
                detail = navaid.detail,
                lat_deg = navaid.latitude,
                lon_deg = navaid.longitude
            )
        }

        val airspaceCounters = mutableMapOf<String, Int>()
        snapshot.airspaces.forEach { airspace ->
            val count = (airspaceCounters[airspace.id] ?: 0) + 1
            airspaceCounters[airspace.id] = count
            val airspaceKey = if (count == 1) airspace.id else "${airspace.id}#$count"

            val minLat = airspace.points.minOf { it.latitude }
            val minLon = airspace.points.minOf { it.longitude }
            val maxLat = airspace.points.maxOf { it.latitude }
            val maxLon = airspace.points.maxOf { it.longitude }

            queries.insertAirspace(
                package_id = packageId,
                airspace_id = airspaceKey,
                name = airspace.name,
                kind = airspace.kind,
                lower_m = airspace.lowerLimitMeters?.toLong(),
                lower_ref = airspace.lowerLimitReference,
                upper_m = airspace.upperLimitMeters?.toLong(),
                upper_ref = airspace.upperLimitReference,
                bbox_min_lat = minLat,
                bbox_min_lon = minLon,
                bbox_max_lat = maxLat,
                bbox_max_lon = maxLon
            )

            airspace.points.forEachIndexed { index, point ->
                queries.insertAirspacePoint(
                    package_id = packageId,
                    airspace_id = airspaceKey,
                    seq = index.toLong(),
                    lat_deg = point.latitude,
                    lon_deg = point.longitude
                )
            }
        }
    }

    private fun activePackageRecord(): ActivePackageRecord? {
        return queries.selectActivePackage { package_id,
                                             _,
                                             _,
                                             _,
                                             _,
                                             _,
                                             map_pmtiles_path,
                                             terrain_pmtiles_path,
                                             _,
                                             _,
                                             _ ->
            ActivePackageRecord(
                packageId = package_id,
                mapPmtilesPath = map_pmtiles_path,
                terrainPmtilesPath = terrain_pmtiles_path
            )
        }.executeAsOneOrNull()
    }

    private fun deletePackageRows(packageId: String) {
        queries.deletePackageAirportRows(packageId)
        queries.deletePackageNavaidRows(packageId)
        queries.deletePackageAirspacePointRows(packageId)
        queries.deletePackageAirspaceRows(packageId)
    }

    private fun resolveSourcePath(source: AviationPackageSource): String? {
        return when (source) {
            is AviationPackageSource.Asset -> assetResolver.resolveToLocalPath(source.assetPath)
            is AviationPackageSource.LocalFile -> source.absolutePath
        }
    }

    private fun parseChecksumFile(raw: String): Map<String, String> {
        return raw.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { line ->
                val parts = line.split(Regex("\\s+"), limit = 2)
                if (parts.size < 2) null else parts[1].trim() to parts[0].trim().lowercase()
            }
            .toMap()
    }

    private fun verifyChecksum(checksumMap: Map<String, String>, path: String, bytes: ByteArray): Boolean {
        val expected = checksumMap[path]?.lowercase() ?: return false
        return hasher.hashHex(bytes).lowercase() == expected
    }

    private fun parseManifest(rawManifestJson: String): ParsedManifest {
        val root = Json.parseToJsonElement(rawManifestJson).jsonObject
        val metadata = root["metadata"]?.jsonObject
        return ParsedManifest(
            cycle = metadata?.readString("cycle"),
            region = metadata?.readString("region"),
            source = root.readString("source"),
            generatedAt = root.readString("generatedAt")
        )
    }

    @OptIn(ExperimentalTime::class)
    private fun nowEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()

    private fun buildPackageId(manifest: ParsedManifest): String {
        val cycle = manifest.cycle ?: "unknown"
        val region = manifest.region ?: "unknown"
        val generatedAt = manifest.generatedAt ?: nowEpochMillis().toString()
        return "$cycle-$region-${generatedAt.filter { it.isLetterOrDigit() || it == '-' }}"
    }

    private fun boundingBox(latitude: Double, longitude: Double, radiusMeters: Double): BoundingBox {
        val latDelta = GeoMath.latitudeDelta(radiusMeters)
        val lonDelta = GeoMath.longitudeDelta(latitude, radiusMeters)
        return BoundingBox(
            minLatitude = latitude - latDelta,
            maxLatitude = latitude + latDelta,
            minLongitude = longitude - lonDelta,
            maxLongitude = longitude + lonDelta
        )
    }

    private data class ActivePackageRecord(
        val packageId: String,
        val mapPmtilesPath: String,
        val terrainPmtilesPath: String
    )

    private data class ParsedManifest(
        val cycle: String?,
        val region: String?,
        val source: String?,
        val generatedAt: String?
    )

    private data class BoundingBox(
        val minLatitude: Double,
        val maxLatitude: Double,
        val minLongitude: Double,
        val maxLongitude: Double
    )

    private companion object {
        const val MANIFEST_PATH = "manifest.json"
        const val CHECKSUMS_PATH = "checksums.sha256"
        const val NAV_XML_PATH = "payload/navsnapshot.xml"
        const val MAP_PMTILES_PATH = "payload/map.pmtiles"
        const val TERRAIN_PMTILES_PATH = "payload/terrain.pmtiles"

        const val PACKAGE_ROOT_DIR = "ofpkg"
        const val MAP_PMTILES_NAME = "map.pmtiles"
        const val TERRAIN_PMTILES_NAME = "terrain.pmtiles"
        const val NAV_XML_NAME = "navsnapshot.xml"
    }
}

private fun pathJoin(base: String, child: String): String {
    val normalizedBase = base.trimEnd('/')
    val normalizedChild = child.trimStart('/')
    return "$normalizedBase/$normalizedChild"
}

private fun JsonObject.readString(key: String): String? = runCatching {
    getValue(key).jsonPrimitive.content
}.getOrNull()

private fun logInstall(message: String) {
    OfpkgInstallLogger.log(message)
}
