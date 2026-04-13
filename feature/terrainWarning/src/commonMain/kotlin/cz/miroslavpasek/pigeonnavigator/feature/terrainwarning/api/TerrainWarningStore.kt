package cz.miroslavpasek.pigeonnavigator.feature.terrainwarning.api

import cz.miroslavpasek.pigeonnavigator.feature.terrainwarning.presentation.TerrainWarningEffect
import cz.miroslavpasek.pigeonnavigator.feature.terrainwarning.presentation.TerrainWarningIntent
import cz.miroslavpasek.pigeonnavigator.feature.terrainwarning.presentation.TerrainWarningState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Exposes state, effects, and inputs for the terrain warning feature store.
 */
interface TerrainWarningStore {
    /** Stream of full terrain warning UI state snapshots. */
    val state: StateFlow<TerrainWarningState>

    /** Stream of one-shot alert effects for platform UI handling. */
    val effects: Flow<TerrainWarningEffect>

    /**
     * Submits an intent to the terrain warning state machine.
     *
     * @param intent User or system input to process.
     */
    fun send(intent: TerrainWarningIntent)

    /** Releases store resources and cancels ongoing work. */
    fun close()
}
