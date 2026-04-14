package cz.miroslavpasek.pigeonnavigator.data.aviation.platform

import platform.Foundation.NSLog

actual object OfpkgInstallLogger {
    actual fun log(message: String) {
        NSLog("[OFPKG] %@", message)
    }
}
