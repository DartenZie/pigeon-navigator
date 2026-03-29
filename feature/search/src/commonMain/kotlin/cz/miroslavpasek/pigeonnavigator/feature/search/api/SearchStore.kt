package cz.miroslavpasek.pigeonnavigator.feature.search.api

import cz.miroslavpasek.pigeonnavigator.feature.search.presentation.SearchEffect
import cz.miroslavpasek.pigeonnavigator.feature.search.presentation.SearchIntent
import cz.miroslavpasek.pigeonnavigator.feature.search.presentation.SearchState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Public entrypoint for interacting with the search feature state machine.
 */
interface SearchStore {
    val state: StateFlow<SearchState>
    val effects: Flow<SearchEffect>

    fun send(intent: SearchIntent)
    fun close()
}
