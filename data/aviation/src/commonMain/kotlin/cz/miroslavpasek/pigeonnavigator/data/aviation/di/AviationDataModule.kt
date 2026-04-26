package cz.miroslavpasek.pigeonnavigator.data.aviation.di

import cz.miroslavpasek.pigeonnavigator.data.aviation.AviationPackageBootstrapper
import cz.miroslavpasek.pigeonnavigator.data.aviation.AviationRepositoryImpl
import cz.miroslavpasek.pigeonnavigator.data.aviation.AviationSearchRepositoryImpl
import cz.miroslavpasek.pigeonnavigator.data.aviation.db.AviationDatabaseProvider
import cz.miroslavpasek.pigeonnavigator.domain.aviation.GetActiveMapPackageUseCase
import cz.miroslavpasek.pigeonnavigator.domain.aviation.InstallAviationPackageUseCase
import cz.miroslavpasek.pigeonnavigator.domain.aviation.QueryContainingAirspacesUseCase
import cz.miroslavpasek.pigeonnavigator.domain.aviation.QueryNearbyAirportsUseCase
import cz.miroslavpasek.pigeonnavigator.domain.aviation.QueryNearbyNavaidsUseCase
import cz.miroslavpasek.pigeonnavigator.domain.aviation.AviationRepository
import cz.miroslavpasek.pigeonnavigator.domain.search.SearchRepository
import org.koin.dsl.module

/**
 * DI bindings for aviation package installation and query data layer.
 */
fun aviationDataModule() = module {
    single { AviationDatabaseProvider().database }
    single<AviationRepository> { AviationRepositoryImpl(database = get()) }
    single<SearchRepository> { AviationSearchRepositoryImpl(database = get()) }
    factory { InstallAviationPackageUseCase(repository = get()) }
    factory { GetActiveMapPackageUseCase(repository = get()) }
    factory { QueryNearbyAirportsUseCase(repository = get()) }
    factory { QueryNearbyNavaidsUseCase(repository = get()) }
    factory { QueryContainingAirspacesUseCase(repository = get()) }
    single { AviationPackageBootstrapper(repository = get()) }
}
