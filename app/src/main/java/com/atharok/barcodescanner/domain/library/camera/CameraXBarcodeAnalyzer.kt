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

import androidx.camera.core.ImageProxy
import com.atharok.barcodescanner.common.extensions.toByteArray
import com.atharok.barcodescanner.presentation.customView.ScanOverlay
import kotlin.math.roundToInt

class CameraXBarcodeAnalyzer(
    barcodeDetector: BarcodeDetector
) : AbstractCameraXBarcodeAnalyzer(barcodeDetector) {

    override fun analyze(image: ImageProxy) {
        try {
            val plane = image.planes[0]
            val imageData = plane.buffer.toByteArray()

            val size = image.width.coerceAtMost(image.height) * ScanOverlay.RATIO

            val left = (image.width - size) / 2f
            val top = (image.height - size) / 2f

            analyse(
                yuvData = imageData,
                dataWidth = plane.rowStride,
                dataHeight = image.height,
                left = left.roundToInt(),
                top = top.roundToInt(),
                width = size.roundToInt(),
                height = size.roundToInt()
            )
        } catch (e: IllegalStateException) {
            // Surface abandoned errors are expected when camera is stopping
            // Just ignore them
        } catch (e: Exception) {
            // Log other unexpected errors but don't crash
            e.printStackTrace()
        } finally {
            image.close()
        }
    }
}