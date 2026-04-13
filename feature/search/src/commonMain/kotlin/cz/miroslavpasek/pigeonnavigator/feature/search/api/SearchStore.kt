package cz.miroslavpasek.pigeonnavigator.feature.search.api

import cz.miroslavpasek.pigeonnavigator.feature.search.presentation.SearchEffect
import cz.miroslavpasek.pigeonnavigator.feature.search.presentation.SearchIntent
import cz.miroslavpasek.pigeonnavigator.feature.search.presentation.SearchState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Exposes state, effects, and inputs for the search feature store.
 */
interface SearchStore {
    /** Stream of full UI state snapshots. */
    val state: StateFlow<SearchState>

    /** Stream of one-shot side effects for UI-only reactions. */
    val effects: Flow<SearchEffect>

    /**
     * Submits an intent to the store.
     *
     * @param intent User or system input to process.
     */
    fun send(intent: SearchIntent)

    /** Releases store resources and cancels ongoing work. */
    fun close()
}
