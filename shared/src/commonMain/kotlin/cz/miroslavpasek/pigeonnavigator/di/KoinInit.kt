package cz.miroslavpasek.pigeonnavigator.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module

fun initKoin(
    appModule: Module? = null
): KoinApplication =
    startKoin {
        modules(
            listOfNotNull(sharedModule, appModule)
        )
    }
