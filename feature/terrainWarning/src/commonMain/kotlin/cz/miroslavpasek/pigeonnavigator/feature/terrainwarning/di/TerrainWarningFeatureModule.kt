package cz.miroslavpasek.pigeonnavigator.feature.terrainwarning.di

import cz.miroslavpasek.pigeonnavigator.domain.terrain.DetectTerrainConflictUseCase
import cz.miroslavpasek.pigeonnavigator.feature.terrainwarning.api.TerrainWarningStore
import cz.miroslavpasek.pigeonnavigator.feature.terrainwarning.internal.RealTerrainWarningStore
import cz.miroslavpasek.pigeonnavigator.feature.terrainwarning.presentation.TerrainWarningReducer
import org.koin.dsl.module

/**
 * Creates DI bindings for terrain warning feature dependencies.
 */
fun terrainWarningFeatureModule() = module {
    factory { DetectTerrainConflictUseCase(terrainRepository = get()) }
    factory { TerrainWarningReducer() }
    factory<TerrainWarningStore> {
        RealTerrainWarningStore(
            detectTerrainConflictUseCase = get(),
            reducer = get(),
            dispatcherProvider = get()
        )
    }
}
