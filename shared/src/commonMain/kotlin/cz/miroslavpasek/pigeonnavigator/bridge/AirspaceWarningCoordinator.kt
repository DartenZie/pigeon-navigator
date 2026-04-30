package cz.miroslavpasek.pigeonnavigator.bridge

import cz.miroslavpasek.pigeonnavigator.core.platform.coroutines.DispatcherProvider
import cz.miroslavpasek.pigeonnavigator.core.util.result.AppResult
import cz.miroslavpasek.pigeonnavigator.domain.aviation.Airspace
import cz.miroslavpasek.pigeonnavigator.domain.aviation.QueryContainingAirspacesUseCase
import cz.miroslavpasek.pigeonnavigator.domain.settings.AppSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

data class AirspaceWarningState(
    val name: String? = null,
    val minutesBeforeEnter: Int = 0
)

class AirspaceWarningCoordinator(
    private val queryContainingAirspacesUseCase: QueryContainingAirspacesUseCase,
    private val appSettingsRepository: AppSettingsRepository,
    private val dispatcherProvider: DispatcherProvider
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.main)
    private val mutableState = MutableStateFlow(AirspaceWarningState())
    private var queryJob: Job? = null

    val state: StateFlow<AirspaceWarningState> = mutableState.asStateFlow()

    fun onLocationUpdated(
        latitude: Double,
        longitude: Double,
        speedMetersPerSecond: Double,
        bearingDegrees: Double
    ) {
        queryJob?.cancel()

        if (speedMetersPerSecond < MIN_WARNING_SPEED_METERS_PER_SECOND) {
            mutableState.update { AirspaceWarningState() }
            return
        }

        val warningSeconds = appSettingsRepository.settings.value.warning.timeToCollisionWarningSeconds
        queryJob = scope.launch(dispatcherProvider.io) {
            val warning = findRestrictedAirspaceAhead(
                latitude = latitude,
                longitude = longitude,
                speedMetersPerSecond = speedMetersPerSecond,
                bearingDegrees = bearingDegrees,
                warningSeconds = warningSeconds
            )
            mutableState.update { warning ?: AirspaceWarningState() }
        }
    }

    fun close() {
        queryJob?.cancel()
        scope.cancel()
    }

    private suspend fun findRestrictedAirspaceAhead(
        latitude: Double,
        longitude: Double,
        speedMetersPerSecond: Double,
        bearingDegrees: Double,
        warningSeconds: Int
    ): AirspaceWarningState? {
        val stepSeconds = minOf(PROJECTION_STEP_SECONDS, maxOf(MIN_PROJECTION_STEP_SECONDS, warningSeconds))
        var secondsAhead = stepSeconds
        while (secondsAhead <= warningSeconds) {
            val projected = projectCoordinate(
                latitude = latitude,
                longitude = longitude,
                bearingDegrees = bearingDegrees,
                distanceMeters = speedMetersPerSecond * secondsAhead
            )
            val result = queryContainingAirspacesUseCase(
                latitude = projected.latitude,
                longitude = projected.longitude
            )
            val restricted = (result as? AppResult.Success)
                ?.value
                ?.firstOrNull { it.isRestrictedAirspace() }
            if (restricted != null) {
                return AirspaceWarningState(
                    name = restricted.name,
                    minutesBeforeEnter = maxOf(1, ceil(secondsAhead / 60.0).toInt())
                )
            }
            secondsAhead += stepSeconds
        }

        return null
    }

    private fun Airspace.isRestrictedAirspace(): Boolean {
        return kind.contains("restricted", ignoreCase = true) ||
            name.contains("restricted", ignoreCase = true)
    }

    private fun projectCoordinate(
        latitude: Double,
        longitude: Double,
        bearingDegrees: Double,
        distanceMeters: Double
    ): ProjectedCoordinate {
        val bearingRadians = bearingDegrees * PI / 180.0
        val latitudeRadians = latitude * PI / 180.0
        val delta = distanceMeters / EARTH_RADIUS_METERS
        val projectedLatitude = latitude + (delta * cos(bearingRadians) * 180.0 / PI)
        val projectedLongitude = longitude + (delta * sin(bearingRadians) * 180.0 / PI / cos(latitudeRadians))
        return ProjectedCoordinate(projectedLatitude, projectedLongitude)
    }

    private data class ProjectedCoordinate(
        val latitude: Double,
        val longitude: Double
    )

    private companion object {
        const val EARTH_RADIUS_METERS = 6_371_000.0
        const val MIN_WARNING_SPEED_METERS_PER_SECOND = 1.0
        const val MIN_PROJECTION_STEP_SECONDS = 5
        const val PROJECTION_STEP_SECONDS = 15
    }
}
