package cz.miroslavpasek.pigeonnavigator

import android.app.Application
import cz.miroslavpasek.pigeonnavigator.data.aviation.AviationPackageBootstrapper
import cz.miroslavpasek.pigeonnavigator.data.aviation.di.aviationDataModule
import cz.miroslavpasek.pigeonnavigator.data.search.di.searchDataModule
import cz.miroslavpasek.pigeonnavigator.data.terrain.di.terrainDataModule
import cz.miroslavpasek.pigeonnavigator.di.appModule
import cz.miroslavpasek.pigeonnavigator.di.dispatcherModule
import cz.miroslavpasek.pigeonnavigator.di.locationModule
import cz.miroslavpasek.pigeonnavigator.di.sharedModule
import cz.miroslavpasek.pigeonnavigator.feature.search.di.searchFeatureModule
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.di.searchDockFeatureModule
import cz.miroslavpasek.pigeonnavigator.feature.terrainwarning.di.terrainWarningFeatureModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PigeonNavigatorApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@PigeonNavigatorApplication)
            modules(
                appModule,
                dispatcherModule,
                locationModule,
                sharedModule,
                aviationDataModule(),
                searchDataModule(),
                searchFeatureModule(),
                searchDockFeatureModule(),
                terrainDataModule(),
                terrainWarningFeatureModule(),
            )
        }

        appScope.launch {
            GlobalContext.get().get<AviationPackageBootstrapper>().ensureInstalledFromAsset()
        }
    }
}
