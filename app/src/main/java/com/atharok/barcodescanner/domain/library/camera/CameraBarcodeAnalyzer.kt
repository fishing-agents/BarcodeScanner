/*
 * Barcode Scanner
 * Copyright (C) 2021  Atharok
 *
 * This file is part of Barcode Scanner.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.atharok.barcodescanner.domain.library.camera

import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.atharok.barcodescanner.domain.library.scan.ScanEngine
import com.google.zxing.Result
import com.google.zxing.ResultPoint

/**
 * Full-frame analyzer: decodes every frame with [engine] and reports it through exactly one
 * [BarcodeDetector.onFrame] call. The engine is [Lazy] so its expensive construction (native
 * libraries, model copy) happens on the analyzer thread on the first frame, not on the UI thread.
 */
class CameraBarcodeAnalyzer(
    private val engine: Lazy<ScanEngine>,
    private val barcodeDetector: BarcodeDetector
) : ImageAnalysis.Analyzer {

    fun interface BarcodeDetector {
        /**
         * Called once per analyzed frame, whether or not anything decoded. [sensorToBuffer] maps
         * camera-sensor coordinates to this frame's buffer (rotation included), for mapping code
         * positions onto the preview.
         */
        fun onFrame(
            codes: List<Result>,
            candidates: List<Array<ResultPoint>>,
            meanLuma: Int,
            frameWidth: Int,
            frameHeight: Int,
            sensorToBuffer: Matrix
        )
    }

    override fun analyze(image: ImageProxy) {
        try {
            val outcome = engine.value.scan(image)
            barcodeDetector.onFrame(
                outcome.codes, outcome.candidates, meanLuma(image),
                image.width, image.height, Matrix(image.imageInfo.sensorToBufferTransformMatrix)
            )
        } catch (e: IllegalStateException) {
            // Surface abandoned while the camera stops: expected, ignore.
        } catch (e: Exception) {
            // A bad frame is dropped; it must not stop scanning.
            Log.w("CameraBarcodeAnalyzer", "Frame analysis failed", e)
        } finally {
            image.close()
        }
    }

    /** Mean brightness of the Y plane, sampled every 8th pixel on each axis. */
    private fun meanLuma(image: ImageProxy): Int {
        val plane = image.planes[0]
        val buffer = plane.buffer
        var sum = 0L
        var count = 0
        for (y in 0 until image.height step 8) {
            val row = y * plane.rowStride
            for (x in 0 until image.width step 8) {
                sum += buffer.get(row + x).toInt() and 0xFF
                count++
            }
        }
        return if (count == 0) 0 else (sum / count).toInt()
    }
}
