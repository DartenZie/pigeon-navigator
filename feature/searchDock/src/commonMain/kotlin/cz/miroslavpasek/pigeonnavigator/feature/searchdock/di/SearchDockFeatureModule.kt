package cz.miroslavpasek.pigeonnavigator.feature.searchdock.di

import cz.miroslavpasek.pigeonnavigator.domain.search.SearchUseCase
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.api.SearchDockStore
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.internal.RealSearchDockStore
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockReducer
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockRouteResolver
import org.koin.dsl.module

fun searchDockFeatureModule() = module {
    factory { SearchUseCase(repository = get()) }
    factory { SearchDockRouteResolver() }
    factory { SearchDockReducer(routeResolver = get()) }
    factory<SearchDockStore> {
        RealSearchDockStore(
            searchUseCase = get(),
            queryNearbyAirportsUseCase = get(),
            queryNearbyNavaidsUseCase = get(),
            reducer = get(),
            dispatcherProvider = get()
        )
    }
}
