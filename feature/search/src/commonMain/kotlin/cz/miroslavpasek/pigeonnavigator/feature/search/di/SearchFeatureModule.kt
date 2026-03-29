package cz.miroslavpasek.pigeonnavigator.feature.search.di

import cz.miroslavpasek.pigeonnavigator.domain.search.SearchUseCase
import cz.miroslavpasek.pigeonnavigator.feature.search.api.SearchStore
import cz.miroslavpasek.pigeonnavigator.feature.search.internal.RealSearchStore
import cz.miroslavpasek.pigeonnavigator.feature.search.presentation.SearchReducer
import org.koin.dsl.module

/**
 * DI module factory for search feature bindings.
 */
fun searchFeatureModule() = module {
    factory { SearchUseCase(repository = get()) }
    factory { SearchReducer() }
    factory<SearchStore> {
        RealSearchStore(
            searchUseCase = get(),
            reducer = get(),
            dispatcherProvider = get()
        )
    }
}
