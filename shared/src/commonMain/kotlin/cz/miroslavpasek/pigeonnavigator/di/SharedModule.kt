package cz.miroslavpasek.pigeonnavigator.di

import cz.miroslavpasek.pigeonnavigator.platform.createLocationService
import cz.miroslavpasek.pigeonnavigator.search.SearchRepository
import cz.miroslavpasek.pigeonnavigator.search.SearchRepositoryImpl
import cz.miroslavpasek.pigeonnavigator.search.SearchUseCase
import cz.miroslavpasek.pigeonnavigator.search.SearchViewModel
import org.koin.dsl.module

val sharedModule = module {
    single { createLocationService() }

    single<SearchRepository> { SearchRepositoryImpl() }
    factory { SearchUseCase(get()) }
    factory { SearchViewModel(get()) }
}