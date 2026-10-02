package com.atharok.barcodescanner.domain.library.scan

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy

/** A barcode decoder that can read a live camera frame or a static image. */
interface ScanEngine {
    fun scan(image: ImageProxy): ScanOutcome
    fun scan(bitmap: Bitmap): ScanOutcome
}
