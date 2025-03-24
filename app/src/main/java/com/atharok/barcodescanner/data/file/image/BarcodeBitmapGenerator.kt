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

package com.atharok.barcodescanner.data.file.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.graphics.createBitmap
import com.atharok.barcodescanner.common.extensions.drawRectangle
import com.atharok.barcodescanner.common.extensions.drawRoundRectangle
import com.atharok.barcodescanner.domain.library.BarcodeImageGeneratorProperties
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

/**
 * Génère l'image d'un code-barres à partir d'un texte.
 */
class BarcodeBitmapGenerator(multiFormatWriter: MultiFormatWriter): BarcodeImageGenerator<Bitmap>(multiFormatWriter) {

    override fun createImageBarcode(
        properties: BarcodeImageGeneratorProperties,
        matrix: BitMatrix
    ): Bitmap {
        val bitmap: Bitmap = createBitmap(properties.width, properties.height)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        createBackground(canvas, paint, properties)
        if(!properties.is2DBarcode)
            createTextContentImage(canvas, paint, properties)
        createBarcodeImage(canvas, paint, matrix, properties)

        return bitmap
    }

    private fun createBackground(
        canvas: Canvas,
        paint: Paint,
        properties: BarcodeImageGeneratorProperties
    ) {
        paint.apply {
            this.color = properties.backgroundColor
            this.isAntiAlias = true
        }

        canvas.drawRectangle(
            left = 0f,
            top = properties.heightF,
            right = properties.widthF,
            bottom = 0f,
            paint = paint
        )
    }

    private fun createBarcodeImage(
        canvas: Canvas,
        paint: Paint,
        matrix: BitMatrix,
        properties: BarcodeImageGeneratorProperties
    ) {
        val margins = properties.marginsPx

        val unitW: Float = (properties.widthF - 2 * margins) / matrix.width.toFloat()
        val unitH: Float = ((properties.heightF - properties.contentsHeight - 2 * margins) / matrix.height.toFloat()).coerceAtLeast(0f)
        val cornerRadius = unitW / 2f * properties.cornerRadius

        paint.apply {
            this.color = properties.frontColor
            this.isAntiAlias = cornerRadius != 0f
        }

        for (x in 0 until matrix.width) {
            for (y in 0 until matrix.height) {
                if (matrix[x, y]) {
                    val left = x.toFloat() * unitW + margins
                    val top = y.toFloat() * unitH + margins
                    val right = left + unitW
                    val bottom = top + unitH

                    canvas.drawRoundRectangle(
                        left = left,
                        top = top,
                        right = right,
                        bottom = bottom,
                        rx = cornerRadius,
                        ry = cornerRadius,
                        paint = paint
                    )
                }
            }
        }
    }

    private fun createTextContentImage(
        canvas: Canvas,
        paint: Paint,
        properties: BarcodeImageGeneratorProperties
    ) {
        paint.apply {
            this.color = properties.frontColor
            this.isAntiAlias = true
            this.textSize = properties.contentsHeight
            this.textAlign = Paint.Align.CENTER
        }

        canvas.drawText(
            properties.contents,
            properties.width / 2f,
            properties.height - properties.marginsPx.toFloat(),
            paint.apply { color = properties.frontColor }
        )

        paint.reset()
    }
}