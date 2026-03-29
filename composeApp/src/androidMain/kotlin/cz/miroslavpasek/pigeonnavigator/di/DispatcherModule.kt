package cz.miroslavpasek.pigeonnavigator.di

import cz.miroslavpasek.pigeonnavigator.core.platform.coroutines.DispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module

private class AndroidDispatcherProvider : DispatcherProvider {
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val default: CoroutineDispatcher = Dispatchers.Default
}

val dispatcherModule = module {
    single<DispatcherProvider> { AndroidDispatcherProvider() }
}
