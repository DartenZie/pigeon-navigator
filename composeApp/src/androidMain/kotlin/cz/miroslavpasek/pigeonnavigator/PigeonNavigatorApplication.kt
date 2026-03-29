package cz.miroslavpasek.pigeonnavigator

import android.app.Application
import cz.miroslavpasek.pigeonnavigator.data.search.di.searchDataModule
import cz.miroslavpasek.pigeonnavigator.di.appModule
import cz.miroslavpasek.pigeonnavigator.di.dispatcherModule
import cz.miroslavpasek.pigeonnavigator.di.locationModule
import cz.miroslavpasek.pigeonnavigator.di.sharedModule
import cz.miroslavpasek.pigeonnavigator.feature.search.di.searchFeatureModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class PigeonNavigatorApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@PigeonNavigatorApplication)
            modules(
                appModule,
                dispatcherModule,
                sharedModule,
                searchDataModule(),
                searchFeatureModule(),
            )
        }
    }
}
