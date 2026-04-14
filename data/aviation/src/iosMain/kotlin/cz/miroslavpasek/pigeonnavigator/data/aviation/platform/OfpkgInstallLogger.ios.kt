package cz.miroslavpasek.pigeonnavigator.data.aviation.platform

import platform.Foundation.NSLog

actual object OfpkgInstallLogger {
    actual fun log(message: String) {
        // NSLog with a Kotlin String as a %@ vararg causes EXC_BAD_ACCESS:
        // the autorelease pool can drain before NSLog dereferences the pointer.
        // Passing the complete string as the format itself (no extra args) is safe
        // because NSLog retains the format NSString for the duration of the call.
        NSLog("[OFPKG] $message")
    }
}
