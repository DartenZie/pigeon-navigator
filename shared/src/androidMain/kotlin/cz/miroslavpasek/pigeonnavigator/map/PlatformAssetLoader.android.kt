package cz.miroslavpasek.pigeonnavigator.map

import android.content.Context
import org.koin.mp.KoinPlatform

actual class PlatformAssetLoader {
    private val context: Context = KoinPlatform.getKoin().get()

    actual fun readText(assetPath: String): String {
        return context.assets.open(assetPath).bufferedReader().use { it.readText() }
    }
}
