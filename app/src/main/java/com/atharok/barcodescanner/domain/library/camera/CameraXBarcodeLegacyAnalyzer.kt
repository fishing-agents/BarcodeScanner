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

/**
 * For API 21 and 22.
 */
class CameraXBarcodeLegacyAnalyzer(
    barcodeDetector: BarcodeDetector
) : AbstractCameraXBarcodeAnalyzer(barcodeDetector) {

    override fun analyze(image: ImageProxy) {
        try {
            val plane = image.planes[0]
            val rotationDegrees = image.imageInfo.rotationDegrees

            val byteArray: ByteArray
            val imageWidth: Int
            val imageHeight: Int

            if (rotationDegrees == 0 || rotationDegrees == 180) {
                byteArray = plane.buffer.toByteArray()
                imageWidth = image.width
                imageHeight = image.height
            } else {
                byteArray = rotateImageArray(plane.buffer.toByteArray(), image.width, image.height, rotationDegrees)
                imageWidth = image.height
                imageHeight = image.width
            }

            val size = imageWidth.coerceAtMost(imageHeight) * ScanOverlay.RATIO

            val left = (imageWidth - size) / 2f
            val top = (imageHeight - size) / 2f

            analyse(
                yuvData = byteArray,
                dataWidth = imageWidth,
                dataHeight = imageHeight,
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

    // 90, 180. 270 rotation
    private fun rotateImageArray(byteArray: ByteArray, width: Int, height: Int, rotationDegrees: Int): ByteArray {
        if (rotationDegrees == 0) return byteArray
        if (rotationDegrees % 90 != 0) return byteArray

        val rotatedByteArray = ByteArray(byteArray.size)
        for (y in 0 until height) {
            for (x in 0 until width) {
                when (rotationDegrees) {
                    90 -> rotatedByteArray[x * height + height - y - 1] = byteArray[x + y * width]
                    180 -> rotatedByteArray[width * (height - y - 1) + width - x - 1] = byteArray[x + y * width]
                    270 -> rotatedByteArray[y + x * height] = byteArray[y * width + width - x - 1]
                }
            }
        }

        return rotatedByteArray
    }

    /*private data class Values(
        val byteArray: ByteArray,
        val imageWidth: Int,
        val imageHeight: Int
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Values

            if (!byteArray.contentEquals(other.byteArray)) return false
            if (imageWidth != other.imageWidth) return false
            if (imageHeight != other.imageHeight) return false

            return true
        }

        /*override fun hashCode(): Int {
            var result = byteArray.contentHashCode()
            result = 31 * result + imageWidth
            result = 31 * result + imageHeight
            return result
        }*/
    }*/
}