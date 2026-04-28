package cz.miroslavpasek.pigeonnavigator.data.settings.di

import cz.miroslavpasek.pigeonnavigator.data.settings.AppSettingsRepositoryImpl
import cz.miroslavpasek.pigeonnavigator.domain.settings.AppSettingsRepository
import org.koin.dsl.module

/**
 * Creates DI bindings for the settings data layer.
 *
 * Platform composition roots are responsible for registering a [KeyValueSettingsStore] before
 * loading this module so the repository can resolve its persistence backend.
 */
fun settingsDataModule() = module {
    single<AppSettingsRepository> {
        AppSettingsRepositoryImpl(
            store = get(),
            dispatcherProvider = get(),
        )
    }
}
