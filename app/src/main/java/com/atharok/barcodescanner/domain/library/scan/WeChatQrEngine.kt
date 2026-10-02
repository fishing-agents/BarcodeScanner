package com.atharok.barcodescanner.domain.library.scan

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.atharok.barcodescanner.wechatqr.WeChatQrNative
import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import com.google.zxing.ResultPoint
import java.nio.ByteBuffer

/**
 * QR-only decoder backed by OpenCV's wechat_qrcode (CNN detector + super-resolution).
 * Used as [TieredScanEngine]'s fallback. Without models it still runs, but finds no
 * candidates, so it then adds little beyond what zxing-cpp already tried.
 */
class WeChatQrEngine(private val native: WeChatQrNative, modelDir: String?) : ScanEngine {

    private val ready = native.nativeInit(modelDir)

    override fun scan(image: ImageProxy): ScanOutcome {
        val plane = image.planes[0]
        return scanGray(plane.buffer, image.width, image.height, plane.rowStride)
    }

    override fun scan(bitmap: Bitmap): ScanOutcome {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val gray = ByteBuffer.allocateDirect(pixels.size)
        for (p in pixels) {
            gray.put((((p shr 16 and 0xFF) * 299 + (p shr 8 and 0xFF) * 587 + (p and 0xFF) * 114) / 1000).toByte())
        }
        gray.rewind()
        return scanGray(gray, bitmap.width, bitmap.height, bitmap.width)
    }

    internal fun scanGray(buffer: ByteBuffer, width: Int, height: Int, rowStride: Int): ScanOutcome {
        if (!ready) return ScanOutcome.EMPTY
        val result = native.nativeDetectAndDecode(buffer, width, height, rowStride)
        val codes = result.texts.mapIndexed { i, text ->
            Result(text, null, toResultPoints(result.decodedQuads[i]), BarcodeFormat.QR_CODE)
        }
        return ScanOutcome(codes, result.candidateQuads.map(::toResultPoints))
    }

    private fun toResultPoints(quad: FloatArray): Array<ResultPoint> =
        Array(4) { ResultPoint(quad[it * 2], quad[it * 2 + 1]) }
}
