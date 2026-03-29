package cz.miroslavpasek.pigeonnavigator.data.search.di

import cz.miroslavpasek.pigeonnavigator.data.search.SearchRepositoryImpl
import cz.miroslavpasek.pigeonnavigator.data.search.local.InMemoryLocalSearchDataSource
import cz.miroslavpasek.pigeonnavigator.data.search.local.LocalSearchDataSource
import cz.miroslavpasek.pigeonnavigator.domain.search.SearchRepository
import org.koin.dsl.module

/**
 * DI module factory for search data bindings.
 */
fun searchDataModule() = module {
    single<LocalSearchDataSource> { InMemoryLocalSearchDataSource() }
    single<SearchRepository> { SearchRepositoryImpl(localDataSource = get()) }
}
