package cz.miroslavpasek.pigeonnavigator.data.aviation.platform

import android.content.Context
import java.io.File
import org.koin.mp.KoinPlatform

actual class PackageAssetResolver {
    private val context: Context = KoinPlatform.getKoin().get()

    actual fun resolveToLocalPath(assetPath: String): String? {
        val targetDir = File(context.filesDir, "ofpkg-assets")
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        val destination = File(targetDir, assetPath.substringAfterLast('/'))
        if (destination.exists() && destination.length() > 0) {
            return destination.absolutePath
        }

        return runCatching {
            context.assets.open(assetPath).use { input ->
                destination.outputStream().use { output ->
                    input.copyTo(output, bufferSize = 64 * 1024)
                }
            }
            destination.absolutePath
        }.getOrNull()
    }
}
