package cz.miroslavpasek.pigeonnavigator.feature.terrainwarning.presentation

import cz.miroslavpasek.pigeonnavigator.domain.failure.Failure
import cz.miroslavpasek.pigeonnavigator.domain.terrain.TerrainWarningLevel

/**
 * Applies pure state transitions for terrain warning presentation.
 */
class TerrainWarningReducer {
    /**
     * Returns the next [TerrainWarningState] for the supplied [intent].
     *
     * This function is side-effect free.
     */
    fun reduce(state: TerrainWarningState, intent: TerrainWarningIntent): TerrainWarningState =
        when (intent) {
            is TerrainWarningIntent.LocationUpdated -> state
            TerrainWarningIntent.ComputationStarted -> {
                state.copy(
                    isComputing = true,
                    lastErrorMessage = null
                )
            }

            is TerrainWarningIntent.ComputationSucceeded -> {
                state.copy(
                    prediction = intent.prediction,
                    warningLevel = intent.prediction.warningLevel,
                    isComputing = false,
                    lastFailure = null,
                    lastErrorMessage = null
                )
            }

            is TerrainWarningIntent.ComputationFailed -> {
                state.copy(
                    isComputing = false,
                    warningLevel = TerrainWarningLevel.None,
                    lastFailure = intent.failure,
                    lastErrorMessage = intent.failure.toMessage()
                )
            }

            TerrainWarningIntent.WarningAcknowledged -> {
                state.copy(warningLevel = TerrainWarningLevel.None)
            }
        }

    private fun Failure.toMessage(): String = when (this) {
        Failure.OutOfCoverage -> "Out of terrain coverage"
        Failure.DataUnavailable -> "Terrain data unavailable"
        is Failure.DataUnavailableReason -> "Terrain data unavailable"
        is Failure.Validation -> message
        Failure.Unexpected -> "Unexpected error"
    }
}
