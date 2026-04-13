package cz.miroslavpasek.pigeonnavigator.di

import cz.miroslavpasek.pigeonnavigator.core.platform.coroutines.DispatcherProvider
import cz.miroslavpasek.pigeonnavigator.data.search.di.searchDataModule
import cz.miroslavpasek.pigeonnavigator.feature.search.api.SearchStore
import cz.miroslavpasek.pigeonnavigator.feature.search.di.searchFeatureModule
import cz.miroslavpasek.pigeonnavigator.feature.search.presentation.SearchEffect
import cz.miroslavpasek.pigeonnavigator.feature.search.presentation.SearchIntent
import cz.miroslavpasek.pigeonnavigator.feature.search.presentation.SearchState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform
import org.koin.dsl.module

private class IosDispatcherProvider : DispatcherProvider {
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val io: CoroutineDispatcher = Dispatchers.Default
    override val default: CoroutineDispatcher = Dispatchers.Default
}

private val iosDispatcherModule = module {
    single<DispatcherProvider> { IosDispatcherProvider() }
}

/**
 * Starts Koin with shared and search feature modules for iOS.
 */
fun initKoin() {
    startKoin {
        modules(
            sharedModule,
            iosDispatcherModule,
            searchDataModule(),
            searchFeatureModule()
        )
    }
}

/**
 * Resolves iOS-facing helper handles from Koin.
 */
class KoinHelper {
    /**
     * Returns a lifecycle-managed bridge over [SearchStore] for Swift UI layers.
     */
    fun getSearchStoreHandle(): SearchStoreHandle = SearchStoreHandle(KoinPlatform.getKoin().get())
}

/**
 * Bridges [SearchStore] flows and intents to a Swift-friendly API surface.
 */
class SearchStoreHandle(
    private val store: SearchStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var stateJob: Job? = null
    private var effectsJob: Job? = null

    /** Starts collecting state updates until [stopState] or [close] is called. */
    fun startState(onEach: (SearchState) -> Unit) {
        if (stateJob != null) return
        stateJob = scope.launch {
            store.state.collect { onEach(it) }
        }
    }

    /** Stops state collection started by [startState]. */
    fun stopState() {
        stateJob?.cancel()
        stateJob = null
    }

    /** Starts collecting one-shot effects until [stopEffects] or [close] is called. */
    fun startEffects(onEach: (SearchEffect) -> Unit) {
        if (effectsJob != null) return
        effectsJob = scope.launch {
            store.effects.collect { onEach(it) }
        }
    }

    /** Stops effect collection started by [startEffects]. */
    fun stopEffects() {
        effectsJob?.cancel()
        effectsJob = null
    }

    /** Forwards query changes to the underlying store. */
    fun onQueryChanged(query: String) {
        store.send(SearchIntent.QueryChanged(query))
    }

    /** Requests search execution for current query state. */
    fun submitSearch() {
        store.send(SearchIntent.SubmitSearch)
    }

    /** Requests clearing current query and results. */
    fun clearSearch() {
        store.send(SearchIntent.ClearSearch)
    }

    /** Stops all collection jobs and closes the underlying store. */
    fun close() {
        stopState()
        stopEffects()
        scope.cancel()
        store.close()
    }
}
