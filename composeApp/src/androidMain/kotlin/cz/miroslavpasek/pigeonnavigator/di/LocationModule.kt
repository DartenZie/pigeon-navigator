package cz.miroslavpasek.pigeonnavigator.di

import cz.miroslavpasek.pigeonnavigator.services.LocationServiceConfig
import cz.miroslavpasek.pigeonnavigator.services.LocationStreamSource
import cz.miroslavpasek.pigeonnavigator.services.UdpLocationListenerConfig
import org.koin.dsl.module

val locationModule = module {
    single {
        LocationServiceConfig(
            source = LocationStreamSource.DeviceGps,
            udpListener = UdpLocationListenerConfig(
                ipAddress = "0.0.0.0",
                port = 49002,
            ),
        )
    }
}
