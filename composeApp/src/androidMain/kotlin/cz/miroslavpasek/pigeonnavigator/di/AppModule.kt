package cz.miroslavpasek.pigeonnavigator.di

import cz.miroslavpasek.pigeonnavigator.ui.viewmodel.HomeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    viewModel { HomeViewModel(locationService = get()) }
}