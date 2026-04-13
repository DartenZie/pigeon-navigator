package cz.miroslavpasek.pigeonnavigator

import android.os.Build

/**
 * Android implementation of [Platform].
 */
class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

/**
 * Returns Android platform descriptor.
 */
actual fun getPlatform(): Platform = AndroidPlatform()
