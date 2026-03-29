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

class KoinHelper {
    fun getSearchStoreHandle(): SearchStoreHandle = SearchStoreHandle(KoinPlatform.getKoin().get())
}

class SearchStoreHandle(
    private val store: SearchStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var stateJob: Job? = null
    private var effectsJob: Job? = null

    fun startState(onEach: (SearchState) -> Unit) {
        if (stateJob != null) return
        stateJob = scope.launch {
            store.state.collect { onEach(it) }
        }
    }

    fun stopState() {
        stateJob?.cancel()
        stateJob = null
    }

    fun startEffects(onEach: (SearchEffect) -> Unit) {
        if (effectsJob != null) return
        effectsJob = scope.launch {
            store.effects.collect { onEach(it) }
        }
    }

    fun stopEffects() {
        effectsJob?.cancel()
        effectsJob = null
    }

    fun onQueryChanged(query: String) {
        store.send(SearchIntent.QueryChanged(query))
    }

    fun submitSearch() {
        store.send(SearchIntent.SubmitSearch)
    }

    fun clearSearch() {
        store.send(SearchIntent.ClearSearch)
    }

    fun close() {
        stopState()
        stopEffects()
        scope.cancel()
        store.close()
    }
}
