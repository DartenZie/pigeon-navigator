package cz.miroslavpasek.pigeonnavigator.feature.searchdock.api

import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockEffect
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockIntent
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SearchDockStore {
    val state: StateFlow<SearchDockState>
    val effects: Flow<SearchDockEffect>
    fun send(intent: SearchDockIntent)
    fun close()
}
