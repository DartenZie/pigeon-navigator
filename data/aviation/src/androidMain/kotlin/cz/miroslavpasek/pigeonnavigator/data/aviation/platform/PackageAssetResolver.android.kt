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
        return runCatching {
            context.assets.open(assetPath).use { input ->
                destination.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            destination.absolutePath
        }.getOrNull()
    }
}
