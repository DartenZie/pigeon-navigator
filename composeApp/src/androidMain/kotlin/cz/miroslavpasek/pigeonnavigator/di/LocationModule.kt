package cz.miroslavpasek.pigeonnavigator.di

import com.google.android.gms.location.LocationServices
import cz.miroslavpasek.pigeonnavigator.services.LocationService
import cz.miroslavpasek.pigeonnavigator.services.LocationServiceAndroid
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val locationModule = module {
    single { LocationServices.getFusedLocationProviderClient(androidContext()) }
    single<LocationService> { LocationServiceAndroid(androidContext(), get()) }
}