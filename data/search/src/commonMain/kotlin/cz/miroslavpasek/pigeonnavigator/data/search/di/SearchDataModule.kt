package cz.miroslavpasek.pigeonnavigator.data.search.di

import cz.miroslavpasek.pigeonnavigator.data.search.SearchRepositoryImpl
import cz.miroslavpasek.pigeonnavigator.data.search.local.InMemoryLocalSearchDataSource
import cz.miroslavpasek.pigeonnavigator.data.search.local.LocalSearchDataSource
import cz.miroslavpasek.pigeonnavigator.domain.search.SearchRepository
import org.koin.dsl.module

/**
 * Creates DI bindings for search data-layer dependencies.
 */
fun searchDataModule() = module {
    single<LocalSearchDataSource> { InMemoryLocalSearchDataSource() }
    single<SearchRepository> { SearchRepositoryImpl(localDataSource = get()) }
}
