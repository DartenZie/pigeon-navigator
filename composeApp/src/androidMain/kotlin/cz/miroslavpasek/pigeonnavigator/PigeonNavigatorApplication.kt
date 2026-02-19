package cz.miroslavpasek.pigeonnavigator

import android.app.Application
import cz.miroslavpasek.pigeonnavigator.di.appModule
import cz.miroslavpasek.pigeonnavigator.di.locationModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class PigeonNavigatorApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@PigeonNavigatorApplication)
            modules(
                appModule,
                locationModule
            )
        }
    }
}