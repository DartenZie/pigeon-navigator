package cz.miroslavpasek.pigeonnavigator.data.terrain.di

import cz.miroslavpasek.pigeonnavigator.data.terrain.ManifestBoundedTerrainDataSource
import cz.miroslavpasek.pigeonnavigator.data.terrain.TerrainElevationDataSource
import cz.miroslavpasek.pigeonnavigator.data.terrain.TerrainRepositoryImpl
import cz.miroslavpasek.pigeonnavigator.data.terrain.TerrariumDemSampler
import cz.miroslavpasek.pigeonnavigator.data.terrain.createTerrariumDemSampler
import cz.miroslavpasek.pigeonnavigator.domain.terrain.TerrainRepository
import org.koin.dsl.module

/**
 * Creates DI bindings for terrain data-layer components.
 */
fun terrainDataModule() = module {
    single<TerrariumDemSampler> { createTerrariumDemSampler() }
    single<TerrainElevationDataSource> { ManifestBoundedTerrainDataSource(sampler = get()) }
    single<TerrainRepository> { TerrainRepositoryImpl(dataSource = get()) }
}
