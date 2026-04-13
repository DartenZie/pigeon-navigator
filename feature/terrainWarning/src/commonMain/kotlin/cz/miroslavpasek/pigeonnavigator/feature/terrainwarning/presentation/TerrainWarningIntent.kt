package cz.miroslavpasek.pigeonnavigator.feature.terrainwarning.presentation

import cz.miroslavpasek.pigeonnavigator.domain.failure.Failure
import cz.miroslavpasek.pigeonnavigator.domain.terrain.AircraftSnapshot
import cz.miroslavpasek.pigeonnavigator.domain.terrain.TerrainConflictPrediction

/**
 * Defines user and internal inputs processed by the terrain warning store.
 */
sealed interface TerrainWarningIntent {
    /** Provides a new aircraft snapshot to evaluate. */
    data class LocationUpdated(val snapshot: AircraftSnapshot) : TerrainWarningIntent

    /** Marks the start of conflict computation. */
    data object ComputationStarted : TerrainWarningIntent

    /** Applies a successful terrain conflict computation. */
    data class ComputationSucceeded(val prediction: TerrainConflictPrediction) : TerrainWarningIntent

    /** Applies a failed terrain conflict computation. */
    data class ComputationFailed(val failure: Failure) : TerrainWarningIntent

    /** Clears active warning state after user acknowledgement. */
    data object WarningAcknowledged : TerrainWarningIntent
}
