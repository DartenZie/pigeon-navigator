package cz.miroslavpasek.pigeonnavigator.feature.search.internal

import cz.miroslavpasek.pigeonnavigator.core.platform.coroutines.DispatcherProvider
import cz.miroslavpasek.pigeonnavigator.core.util.result.AppResult
import cz.miroslavpasek.pigeonnavigator.domain.failure.Failure
import cz.miroslavpasek.pigeonnavigator.domain.search.SearchUseCase
import cz.miroslavpasek.pigeonnavigator.feature.search.api.SearchStore
import cz.miroslavpasek.pigeonnavigator.feature.search.presentation.SearchEffect
import cz.miroslavpasek.pigeonnavigator.feature.search.presentation.SearchIntent
import cz.miroslavpasek.pigeonnavigator.feature.search.presentation.SearchReducer
import cz.miroslavpasek.pigeonnavigator.feature.search.presentation.SearchState
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

/**
 * Coordinates search side effects and state updates for [SearchStore].
 *
 * The store runs search work on the injected IO dispatcher and emits state updates on the
 * store scope dispatcher.
 */
internal class RealSearchStore(
    private val searchUseCase: SearchUseCase,
    private val reducer: SearchReducer,
    private val dispatcherProvider: DispatcherProvider
) : SearchStore {

    private val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.main)
    private val mutableState = MutableStateFlow(SearchState())
    private val effectChannel = Channel<SearchEffect>(capacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override val state: StateFlow<SearchState> = mutableState.asStateFlow()
    override val effects: Flow<SearchEffect> = effectChannel.receiveAsFlow()

    /**
     * Processes incoming intents, delegating search execution for submit events.
     */
    override fun send(intent: SearchIntent) {
        when (intent) {
            SearchIntent.SubmitSearch -> submitCurrentQuery()
            else -> reduce(intent)
        }
    }

    /** Cancels active coroutines and closes the effect channel. */
    override fun close() {
        scope.cancel()
        effectChannel.close()
    }

    private fun submitCurrentQuery() {
        reduce(SearchIntent.SubmitSearch)
        val currentQuery = state.value.query
        scope.launch(dispatcherProvider.io) {
            when (val result = searchUseCase(currentQuery)) {
                is AppResult.Success -> {
                    reduce(SearchIntent.SearchSucceeded(result.value))
                }

                is AppResult.Failure -> {
                    handleFailure(result.error)
                }
            }
        }
    }

    private fun handleFailure(error: Failure) {
        when (error) {
            is Failure.Validation -> {
                reduce(SearchIntent.SearchFailed(error.message))
                effectChannel.trySend(SearchEffect.ShowValidationError(error.message))
            }

            Failure.Unexpected -> {
                val message = "Unexpected error"
                reduce(SearchIntent.SearchFailed(message))
                effectChannel.trySend(SearchEffect.ShowUnexpectedError(message))
            }

            Failure.OutOfCoverage,
            Failure.DataUnavailable -> {
                val message = "Data unavailable"
                reduce(SearchIntent.SearchFailed(message))
                effectChannel.trySend(SearchEffect.ShowUnexpectedError(message))
            }
        }
    }

    private fun reduce(intent: SearchIntent) {
        mutableState.value = reducer.reduce(mutableState.value, intent)
    }
}
