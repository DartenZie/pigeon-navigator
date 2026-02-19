package cz.miroslavpasek.pigeonnavigator.di

import cz.miroslavpasek.pigeonnavigator.platform.createLocationService
import org.koin.dsl.module

val sharedModule = module {
    single { createLocationService() }
}