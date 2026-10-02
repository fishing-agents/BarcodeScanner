package com.atharok.barcodescanner.domain.library.scan

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import zxingcpp.BarcodeReader

/** Primary decoder: zxing-cpp over every ZXing-representable format, multi-code, with inverted/rotated retries. */
class ZxingCppEngine : ScanEngine {

    private val reader = BarcodeReader(
        BarcodeReader.Options(
            formats = ZxingCppFormatMapper.SUPPORTED,
            tryHarder = true,
            tryRotate = true,
            tryInvert = true,
            maxNumberOfSymbols = 8,
            returnErrors = true
        )
    )

    override fun scan(image: ImageProxy): ScanOutcome = ZxingCppResultMapper.toOutcome(reader.read(image))

    override fun scan(bitmap: Bitmap): ScanOutcome = ZxingCppResultMapper.toOutcome(reader.read(bitmap))
}
