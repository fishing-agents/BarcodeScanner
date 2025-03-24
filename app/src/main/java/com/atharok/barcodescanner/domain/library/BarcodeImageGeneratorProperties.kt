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

import android.graphics.Color
import androidx.annotation.ColorInt
import com.atharok.barcodescanner.common.extensions.is2DBarcode
import com.atharok.barcodescanner.common.utils.BARCODE_1_DIMENSION_IMAGE_DEFAULT_MARGINS_PX
import com.atharok.barcodescanner.common.utils.BARCODE_2_DIMENSION_IMAGE_DEFAULT_MARGINS_PX
import com.atharok.barcodescanner.common.utils.BARCODE_IMAGE_DEFAULT_SIZE
import com.atharok.barcodescanner.common.utils.ENCODING_ISO_8859_1
import com.atharok.barcodescanner.common.utils.ENCODING_UTF_8
import com.atharok.barcodescanner.domain.entity.barcode.QrCodeErrorCorrectionLevel
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import java.io.Serializable

data class BarcodeImageGeneratorProperties(
    val contents: String,
    val format: BarcodeFormat,
    val qrCodeErrorCorrectionLevel: QrCodeErrorCorrectionLevel? = null,
    var marginsPx: Int = when {
        format.is2DBarcode() -> BARCODE_2_DIMENSION_IMAGE_DEFAULT_MARGINS_PX
        else -> BARCODE_1_DIMENSION_IMAGE_DEFAULT_MARGINS_PX
    },
    private val size: Int = BARCODE_IMAGE_DEFAULT_SIZE,
    @ColorInt var frontColor: Int = Color.BLACK,
    @ColorInt var backgroundColor: Int = Color.WHITE
): Serializable {

    val is2DBarcode: Boolean = format.is2DBarcode()

    var width: Int = size
    var height: Int = if(is2DBarcode && format != BarcodeFormat.PDF_417) size else size / 2
    val widthF: Float get() = width.toFloat()
    val heightF: Float get() = height.toFloat()

    val contentsHeight: Float get() = if(is2DBarcode) 0f else (width / (contents.length + 2f)) // ajuster la taille du texte en fonction de la largeur de l'image et du contenu

    var cornerRadius: Float = 0.0f
        set(value) {
            field = if(value < 0f) 0f else if(value > 1f) 1f else value
        }

    val hints: Map<EncodeHintType, Any>
        get() {
            val encoding: String = when(format) {
                BarcodeFormat.QR_CODE, BarcodeFormat.PDF_417 -> ENCODING_UTF_8
                else -> ENCODING_ISO_8859_1
            }

            return qrCodeErrorCorrectionLevel?.errorCorrectionLevel?.let {
                mapOf<EncodeHintType, Any>(EncodeHintType.CHARACTER_SET to encoding, EncodeHintType.MARGIN to 0, EncodeHintType.ERROR_CORRECTION to it)
            } ?: run {
                mapOf<EncodeHintType, Any>(EncodeHintType.CHARACTER_SET to encoding, EncodeHintType.MARGIN to 0)
            }
        }
}