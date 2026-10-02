package com.atharok.barcodescanner.wechatqr

import android.content.Context
import java.io.File

/**
 * Copies the bundled WeChatQRCode models out of APK assets (OpenCV needs real file paths)
 * and returns their directory, or null when the models aren't bundled. Idempotent: files
 * already copied are kept. Does file I/O — call off the UI thread.
 */
object WeChatQrModelInstaller {

    private const val ASSET_DIR = "wechat_qrcode"
    private val MODEL_FILES = listOf("detect.prototxt", "detect.caffemodel", "sr.prototxt", "sr.caffemodel")

    fun ensureInstalled(context: Context): String? {
        val bundled = context.assets.list(ASSET_DIR)?.toSet() ?: return null
        if (!bundled.containsAll(MODEL_FILES)) return null

        val dir = File(context.filesDir, ASSET_DIR).apply { mkdirs() }
        for (name in MODEL_FILES) {
            val target = File(dir, name)
            if (target.exists()) continue
            val partial = File(dir, "$name.part")
            context.assets.open("$ASSET_DIR/$name").use { input -> partial.outputStream().use(input::copyTo) }
            partial.renameTo(target)
        }
        return dir.path
    }
}
