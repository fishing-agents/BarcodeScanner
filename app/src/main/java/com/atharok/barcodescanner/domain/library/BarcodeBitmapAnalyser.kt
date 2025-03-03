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

package com.atharok.barcodescanner.domain.library

import android.graphics.Bitmap
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer


/**
 * Search for a barcode in an image (Bitmap).
 */
class BarcodeBitmapAnalyser {

    private val reader = MultiFormatReader().apply {
        setHints(mapOf(DecodeHintType.TRY_HARDER to true))
    }

    fun detectBarcodeFromBitmap(bitmap: Bitmap): Result? {

        var result: Result? = detect(bitmap)

        // If the barcode is not detected, the image is reanalyzed with different scales.
        // Adjusting the image scale can improve detection in some cases.
        for(i in 1..3) {
            if(result != null) {
                break
            }
            val bitmapRescaled = rescaleBitmap(bitmap, i / 4f)
            result = detect(bitmapRescaled)
        }

        return result
    }

    private fun detect(bitmap: Bitmap): Result? {
        val width = bitmap.width
        val height = bitmap.height
        val size = width * height
        val bitmapBuffer = IntArray(size)

        bitmap.getPixels(bitmapBuffer, 0, width, 0, 0, width, height)

        val source = RGBLuminanceSource(width, height, bitmapBuffer)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

        reader.reset()

        return try {
            reader.decode(binaryBitmap)
        } catch (e: NotFoundException) {
            val invertedSource = source.invert()
            val invertedBinaryBitmap = BinaryBitmap(HybridBinarizer(invertedSource))
            reader.reset()
            try {
                reader.decode(invertedBinaryBitmap)
            } catch (e: ReaderException) {
                null
            }
        }
    }

    private fun rescaleBitmap(bitmap: Bitmap, scale: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        return Bitmap.createScaledBitmap(bitmap, (width * scale).toInt(), (height * scale).toInt(), true)
    }
}