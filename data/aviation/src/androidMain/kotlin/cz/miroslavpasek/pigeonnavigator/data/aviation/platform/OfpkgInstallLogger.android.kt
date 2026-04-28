package cz.miroslavpasek.pigeonnavigator.data.aviation.platform

import android.util.Log

actual object OfpkgInstallLogger {
    actual fun log(message: String) {
        Log.i(TAG, message)
    }

    private const val TAG = "OFPKG"
}
