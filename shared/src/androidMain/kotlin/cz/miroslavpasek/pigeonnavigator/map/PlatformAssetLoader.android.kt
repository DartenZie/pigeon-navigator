package cz.miroslavpasek.pigeonnavigator.map

import android.content.Context
import org.koin.mp.KoinPlatform

/**
 * Android asset-backed implementation of [PlatformAssetLoader].
 */
actual class PlatformAssetLoader {
    private val context: Context = KoinPlatform.getKoin().get()

    /**
     * Reads text content from an Android assets path.
     */
    actual fun readText(assetPath: String): String {
        return context.assets.open(assetPath).bufferedReader().use { it.readText() }
    }
}
