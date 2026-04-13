package cz.miroslavpasek.pigeonnavigator.feature.terrainwarning.presentation

import cz.miroslavpasek.pigeonnavigator.domain.terrain.TerrainConflictPrediction
import cz.miroslavpasek.pigeonnavigator.domain.terrain.TerrainWarningLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class TerrainWarningReducerTest {

    private val reducer = TerrainWarningReducer()

    @Test
    fun setsComputingFlagOnStart() {
        val updated = reducer.reduce(TerrainWarningState(), TerrainWarningIntent.ComputationStarted)
        assertEquals(true, updated.isComputing)
        assertNull(updated.lastErrorMessage)
    }

    @Test
    fun storesPredictionOnSuccess() {
        val prediction = TerrainConflictPrediction(
            hasConflict = true,
            warningLevel = TerrainWarningLevel.Caution,
            minClearanceMeters = 40.0,
            distanceToImpactMeters = 1_100.0,
            timeToImpactSeconds = 22.0
        )

        val updated = reducer.reduce(
            TerrainWarningState(isComputing = true),
            TerrainWarningIntent.ComputationSucceeded(prediction)
        )

        assertFalse(updated.isComputing)
        assertEquals(TerrainWarningLevel.Caution, updated.warningLevel)
        assertEquals(prediction, updated.prediction)
    }
}
