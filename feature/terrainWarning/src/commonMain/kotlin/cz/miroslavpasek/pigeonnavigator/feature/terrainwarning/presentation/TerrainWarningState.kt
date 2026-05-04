package cz.miroslavpasek.pigeonnavigator.feature.terrainwarning.presentation

import cz.miroslavpasek.pigeonnavigator.domain.failure.Failure
import cz.miroslavpasek.pigeonnavigator.domain.terrain.TerrainConflictPrediction
import cz.miroslavpasek.pigeonnavigator.domain.terrain.TerrainWarningLevel

/**
 * Represents the full immutable UI state for terrain warning presentation.
 *
 * @property prediction Last successful terrain conflict prediction, or `null` before first success.
 * @property warningLevel Current warning level reflected in the UI.
 * @property isComputing True while a terrain conflict computation is running.
 * @property lastFailure Last computation failure, or `null` when no failure is active.
 * @property lastErrorMessage Last user-visible error message, or `null` when no error is active.
 */
data class TerrainWarningState(
    val prediction: TerrainConflictPrediction? = null,
    val warningLevel: TerrainWarningLevel = TerrainWarningLevel.None,
    val isComputing: Boolean = false,
    val lastFailure: Failure? = null,
    val lastErrorMessage: String? = null
)
