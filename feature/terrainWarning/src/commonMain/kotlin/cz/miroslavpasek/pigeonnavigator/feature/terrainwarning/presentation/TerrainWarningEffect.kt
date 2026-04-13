package cz.miroslavpasek.pigeonnavigator.feature.terrainwarning.presentation

import cz.miroslavpasek.pigeonnavigator.domain.terrain.TerrainConflictPrediction

/**
 * Defines one-shot alert effects emitted by the terrain warning store.
 */
sealed interface TerrainWarningEffect {
    /** Requests a non-warning alert for [prediction]. */
    data class TriggerCaution(val prediction: TerrainConflictPrediction) : TerrainWarningEffect

    /** Requests a warning-level alert for [prediction]. */
    data class TriggerWarning(val prediction: TerrainConflictPrediction) : TerrainWarningEffect

    /** Requests clearing any active warning UI. */
    data object ClearWarning : TerrainWarningEffect
}
