package cz.miroslavpasek.pigeonnavigator.map

import android.content.Context
import java.io.File
import org.koin.mp.KoinPlatform

actual class TileArchiveFileStore {
    private val context: Context = KoinPlatform.getKoin().get()
    private val archivesDir = File(context.filesDir, "pmtiles")

    actual fun ensureLocalFileUrl(assetPath: String): String {
        if (!archivesDir.exists()) {
            archivesDir.mkdirs()
        }

        val fileName = File(assetPath).name
        val destination = File(archivesDir, fileName)
        val assetSize = getAssetSize(assetPath)

        if (!destination.exists() || (assetSize != null && destination.length() != assetSize)) {
            context.assets.open(assetPath).use { input ->
                destination.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        return "file://${destination.absolutePath}"
    }

    private fun getAssetSize(assetPath: String): Long? = runCatching {
        context.assets.openFd(assetPath).use { it.length }
    }.getOrNull()
}
