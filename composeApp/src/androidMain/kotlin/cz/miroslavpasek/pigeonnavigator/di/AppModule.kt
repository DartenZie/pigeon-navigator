package cz.miroslavpasek.pigeonnavigator.di

import cz.miroslavpasek.pigeonnavigator.bridge.MapTapLookupCoordinator
import cz.miroslavpasek.pigeonnavigator.ui.viewmodel.HomeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    factory {
        MapTapLookupCoordinator(
            queryNearbyAirportsUseCase = get(),
            queryContainingAirspacesUseCase = get(),
            queryNearbyNavaidsUseCase = get(),
            dispatcherProvider = get()
        )
    }

    viewModel {
        HomeViewModel(
            locationService = get(),
            terrainWarningStore = get(),
            mapTapLookupCoordinator = get(),
            searchDockStore = get()
        )
    }
}
