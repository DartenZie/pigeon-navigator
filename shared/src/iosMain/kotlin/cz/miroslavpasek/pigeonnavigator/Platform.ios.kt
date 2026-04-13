package cz.miroslavpasek.pigeonnavigator

import platform.UIKit.UIDevice

/**
 * iOS implementation of [Platform].
 */
class IOSPlatform : Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

/**
 * Returns iOS platform descriptor.
 */
actual fun getPlatform(): Platform = IOSPlatform()
