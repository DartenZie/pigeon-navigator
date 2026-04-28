package cz.miroslavpasek.pigeonnavigator.feature.terrainwarning.internal

import cz.miroslavpasek.pigeonnavigator.core.platform.coroutines.DispatcherProvider
import cz.miroslavpasek.pigeonnavigator.core.util.result.AppResult
import cz.miroslavpasek.pigeonnavigator.domain.settings.AppSettingsRepository
import cz.miroslavpasek.pigeonnavigator.domain.terrain.AircraftSnapshot
import cz.miroslavpasek.pigeonnavigator.domain.terrain.DetectTerrainConflictUseCase
import cz.miroslavpasek.pigeonnavigator.domain.terrain.TerrainConflictPrediction
import cz.miroslavpasek.pigeonnavigator.domain.terrain.TerrainConflictParameters
import cz.miroslavpasek.pigeonnavigator.domain.terrain.TerrainWarningLevel
import cz.miroslavpasek.pigeonnavigator.feature.terrainwarning.api.TerrainWarningStore
import cz.miroslavpasek.pigeonnavigator.feature.terrainwarning.presentation.TerrainWarningEffect
import cz.miroslavpasek.pigeonnavigator.feature.terrainwarning.presentation.TerrainWarningIntent
import cz.miroslavpasek.pigeonnavigator.feature.terrainwarning.presentation.TerrainWarningReducer
import cz.miroslavpasek.pigeonnavigator.feature.terrainwarning.presentation.TerrainWarningState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Coordinates terrain conflict computations and publishes state/effects for the UI.
 *
 * The store throttles recomputation to meaningful telemetry changes and executes terrain
 * sampling on the injected default dispatcher.
 */
internal class RealTerrainWarningStore(
    private val detectTerrainConflictUseCase: DetectTerrainConflictUseCase,
    private val reducer: TerrainWarningReducer,
    private val dispatcherProvider: DispatcherProvider,
    private val appSettingsRepository: AppSettingsRepository,
    private val baseParameters: TerrainConflictParameters = TerrainConflictParameters()
) : TerrainWarningStore {

    private val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.main)
    private val mutableState = MutableStateFlow(TerrainWarningState())
    private val effectChannel = Channel<TerrainWarningEffect>(capacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private var lastSnapshot: AircraftSnapshot? = null
    private var lastAlertLevel: TerrainWarningLevel = TerrainWarningLevel.None

    override val state: StateFlow<TerrainWarningState> = mutableState.asStateFlow()
    override val effects: Flow<TerrainWarningEffect> = effectChannel.receiveAsFlow()

    /** Routes intents and triggers computation for location updates. */
    override fun send(intent: TerrainWarningIntent) {
        when (intent) {
            is TerrainWarningIntent.LocationUpdated -> handleLocation(intent.snapshot)
            else -> reduce(intent)
        }
    }

    /** Cancels active work and closes effect streaming resources. */
    override fun close() {
        scope.cancel()
        effectChannel.close()
    }

    private fun handleLocation(snapshot: AircraftSnapshot) {
        if (!shouldRecompute(lastSnapshot, snapshot)) {
            return
        }

        lastSnapshot = snapshot
        reduce(TerrainWarningIntent.ComputationStarted)

        val parameters = currentParameters()

        scope.launch(dispatcherProvider.default) {
            when (val result = detectTerrainConflictUseCase(snapshot, parameters)) {
                is AppResult.Success -> {
                    reduce(TerrainWarningIntent.ComputationSucceeded(result.value))
                    emitWarningEffect(result.value.warningLevel, result.value)
                }

                is AppResult.Failure -> {
                    reduce(TerrainWarningIntent.ComputationFailed(result.error))
                }
            }
        }
    }

    private fun emitWarningEffect(
        level: TerrainWarningLevel,
        prediction: TerrainConflictPrediction
    ) {
        if (level == lastAlertLevel) {
            return
        }

        when (level) {
            TerrainWarningLevel.Warning -> effectChannel.trySend(TerrainWarningEffect.TriggerWarning(prediction))
            TerrainWarningLevel.Caution,
            TerrainWarningLevel.Info -> effectChannel.trySend(TerrainWarningEffect.TriggerCaution(prediction))
            TerrainWarningLevel.None -> effectChannel.trySend(TerrainWarningEffect.ClearWarning)
        }

        lastAlertLevel = level
    }

    private fun reduce(intent: TerrainWarningIntent) {
        mutableState.value = reducer.reduce(mutableState.value, intent)
    }

    /**
     * Returns the conflict parameters to use for the next computation, with the
     * user-configured time-to-collision warning threshold layered over [baseParameters].
     *
     * Reading the latest [AppSettingsRepository.settings] value here keeps this side-effect-free
     * (no flow collection) while still picking up live preference changes between samples.
     *
     * Visible to module tests so settings wiring can be asserted without driving terrain sampling.
     */
    internal fun currentParameters(): TerrainConflictParameters {
        val warningSeconds = appSettingsRepository.settings.value
            .warning.timeToCollisionWarningSeconds
        return baseParameters.copy(warningTimeToImpactSeconds = warningSeconds.toDouble())
    }

    /**
     * Returns `true` when telemetry changed enough to justify recomputing conflict prediction.
     */
    private fun shouldRecompute(previous: AircraftSnapshot?, current: AircraftSnapshot): Boolean {
        if (previous == null) {
            return true
        }

        val distanceDeltaMeters = haversineMeters(
            previous.latitude,
            previous.longitude,
            current.latitude,
            current.longitude
        )

        val bearingDelta = angleDeltaDegrees(previous.bearingDegrees, current.bearingDegrees)
        val altitudeDeltaMeters = abs(current.altitudeMeters - previous.altitudeMeters)

        return distanceDeltaMeters >= MIN_DISTANCE_DELTA_METERS ||
            bearingDelta >= MIN_BEARING_DELTA_DEGREES ||
            altitudeDeltaMeters >= MIN_ALTITUDE_DELTA_METERS
    }

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = (lat2 - lat1).toRadians()
        val dLon = (lon2 - lon1).toRadians()
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(lat1.toRadians()) * cos(lat2.toRadians()) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    private fun Double.toRadians(): Double = this * PI / 180.0

    private fun angleDeltaDegrees(a: Double, b: Double): Double {
        val normalized = ((b - a + 540.0) % 360.0) - 180.0
        return abs(normalized)
    }

    private companion object {
        const val MIN_DISTANCE_DELTA_METERS = 40.0
        const val MIN_BEARING_DELTA_DEGREES = 4.0
        const val MIN_ALTITUDE_DELTA_METERS = 20.0
        const val EARTH_RADIUS_METERS = 6_371_000.0
    }
}
