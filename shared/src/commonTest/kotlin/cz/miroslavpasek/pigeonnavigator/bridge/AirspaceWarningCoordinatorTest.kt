package cz.miroslavpasek.pigeonnavigator.bridge

import cz.miroslavpasek.pigeonnavigator.core.platform.coroutines.DispatcherProvider
import cz.miroslavpasek.pigeonnavigator.core.util.result.AppResult
import cz.miroslavpasek.pigeonnavigator.domain.aviation.ActiveMapPackage
import cz.miroslavpasek.pigeonnavigator.domain.aviation.Airspace
import cz.miroslavpasek.pigeonnavigator.domain.aviation.AviationPackageSource
import cz.miroslavpasek.pigeonnavigator.domain.aviation.AviationRepository
import cz.miroslavpasek.pigeonnavigator.domain.aviation.GeoPoint
import cz.miroslavpasek.pigeonnavigator.domain.aviation.NearbyAirport
import cz.miroslavpasek.pigeonnavigator.domain.aviation.NearbyNavaid
import cz.miroslavpasek.pigeonnavigator.domain.aviation.QueryContainingAirspacesUseCase
import cz.miroslavpasek.pigeonnavigator.domain.failure.Failure
import cz.miroslavpasek.pigeonnavigator.domain.settings.AppSettings
import cz.miroslavpasek.pigeonnavigator.domain.settings.AppSettingsRepository
import cz.miroslavpasek.pigeonnavigator.domain.settings.MapPreferences
import cz.miroslavpasek.pigeonnavigator.domain.settings.SearchPreferences
import cz.miroslavpasek.pigeonnavigator.domain.settings.UnitPreferences
import cz.miroslavpasek.pigeonnavigator.domain.settings.WarningPreferences
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class AirspaceWarningCoordinatorTest {

    @Test
    fun warnsWhenProjectedTrackEntersCtrAirspace() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val repository = ProjectedAirspaceRepository(
            airspaces = listOf(
                Airspace(
                    id = "LKKB-CTR",
                    name = "Kbely CTR",
                    kind = "CTR",
                    lowerLimitMeters = null,
                    lowerLimitReference = null,
                    upperLimitMeters = null,
                    upperLimitReference = null,
                    points = listOf(
                        GeoPoint(50.0, 14.0),
                        GeoPoint(50.0, 15.0),
                        GeoPoint(51.0, 15.0)
                    )
                )
            ),
            minimumLongitude = 0.095
        )
        val coordinator = AirspaceWarningCoordinator(
            queryContainingAirspacesUseCase = QueryContainingAirspacesUseCase(repository = repository),
            appSettingsRepository = StubSettingsRepository(
                AppSettings(warning = WarningPreferences(timeToCollisionWarningSeconds = 60))
            ),
            dispatcherProvider = AirspaceTestDispatcherProvider(dispatcher)
        )

        coordinator.onLocationUpdated(
            latitude = 50.0,
            longitude = 0.0,
            speedMetersPerSecond = 30.0,
            bearingDegrees = 90.0
        )
        advanceUntilIdle()

        assertEquals("Kbely CTR", coordinator.state.value.name)
        assertEquals(4, coordinator.state.value.minutesBeforeEnter)

        coordinator.close()
    }
}

private class AirspaceTestDispatcherProvider(
    dispatcher: CoroutineDispatcher
) : DispatcherProvider {
    override val main: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
    override val default: CoroutineDispatcher = dispatcher
}

private class StubSettingsRepository(
    initialSettings: AppSettings
) : AppSettingsRepository {
    override val settings: StateFlow<AppSettings> = MutableStateFlow(initialSettings)

    override suspend fun updateUnits(units: UnitPreferences): AppResult<Unit, Failure> = AppResult.Success(Unit)

    override suspend fun updateWarningPreferences(warning: WarningPreferences): AppResult<Unit, Failure> = AppResult.Success(Unit)

    override suspend fun updateSearchPreferences(search: SearchPreferences): AppResult<Unit, Failure> = AppResult.Success(Unit)

    override suspend fun updateMapPreferences(map: MapPreferences): AppResult<Unit, Failure> = AppResult.Success(Unit)

    override suspend fun updateLocationPreferences(
        location: cz.miroslavpasek.pigeonnavigator.domain.settings.LocationPreferences
    ): AppResult<Unit, Failure> = AppResult.Success(Unit)
}

private class ProjectedAirspaceRepository(
    private val airspaces: List<Airspace>,
    private val minimumLongitude: Double
) : AviationRepository {
    override suspend fun installPackage(source: AviationPackageSource): AppResult<Unit, Failure> = AppResult.Success(Unit)

    override suspend fun getActiveMapPackage(): AppResult<ActiveMapPackage, Failure> {
        return AppResult.Success(
            ActiveMapPackage(
                packageId = "pkg",
                mapPmtilesAbsolutePath = "/tmp/map.pmtiles",
                terrainPmtilesAbsolutePath = "/tmp/terrain.pmtiles"
            )
        )
    }

    override suspend fun activePackageFilesExist(): Boolean = true

    override suspend fun nearbyAirports(
        latitude: Double,
        longitude: Double,
        radiusMeters: Double,
        limit: Int
    ): AppResult<List<NearbyAirport>, Failure> = AppResult.Success(emptyList())

    override suspend fun nearbyNavaids(
        latitude: Double,
        longitude: Double,
        radiusMeters: Double,
        limit: Int
    ): AppResult<List<NearbyNavaid>, Failure> = AppResult.Success(emptyList())

    override suspend fun containingAirspaces(
        latitude: Double,
        longitude: Double
    ): AppResult<List<Airspace>, Failure> {
        return AppResult.Success(if (longitude >= minimumLongitude) airspaces else emptyList())
    }
}
